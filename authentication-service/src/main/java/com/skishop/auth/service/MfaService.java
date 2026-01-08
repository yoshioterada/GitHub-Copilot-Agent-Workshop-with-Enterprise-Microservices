package com.skishop.auth.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.skishop.auth.dto.LoginResponse;
import com.skishop.auth.dto.MfaSetupRequest;
import com.skishop.auth.dto.MfaSetupResponse;
import com.skishop.auth.dto.MfaVerificationRequest;
import com.skishop.auth.dto.MfaType;
import com.skishop.auth.entity.UserMFA;
import com.skishop.auth.service.azure.AzureMfaService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * MFA (Multi-Factor Authentication) Service
 * Implements two-factor authentication using TOTP (Time-based One-Time Password) and Azure Entra ID MFA
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MfaService {

    private final EntityManager entityManager;
    private final GoogleAuthenticator googleAuthenticator;
    private final AzureMfaService azureMfaService;
    
    // Manual log field since Lombok @Slf4j may not be working properly
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MfaService.class);
    
    // Default application name for QR codes
    private static final String DEFAULT_ISSUER = "SkiShop";
    private static final int RECOVERY_CODES_COUNT = 10;
    private static final int RECOVERY_CODE_LENGTH = 8;

    /**
     * Verify MFA code using TOTP or Azure Entra ID
     */
    public boolean verifyMfaCode(UUID userId, String code) {
        try {
            // Get UserMFA entity
            UserMFA userMFA = entityManager.createQuery(
                "SELECT um FROM UserMFA um WHERE um.user.id = :userId AND um.isEnabled = true", 
                UserMFA.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .orElse(null);

            if (userMFA == null) {
                log.warn("MFA not enabled for user: {}", userId);
                return false;
            }

            // First check if it's a recovery code
            if (isRecoveryCode(userMFA, code)) {
                log.info("Recovery code used for user: {}", userId);
                userMFA.updateLastUsed();
                return true;
            }

            // Route to appropriate MFA verification based on type
            boolean isValid = false;
            if (MfaType.AZURE_ENTRA_ID.getValue().equals(userMFA.getMfaType())) {
                isValid = verifyAzureEntraIdMfa(userMFA, code);
            } else {
                // Default to TOTP verification
                isValid = verifyTotpMfa(userMFA, code);
            }

            if (isValid) {
                log.info("MFA code verified for user: {} using {}", userId, userMFA.getMfaType());
                userMFA.updateLastUsed();
                entityManager.merge(userMFA);
                return true;
            }

            log.warn("Invalid MFA code for user: {}", userId);
            return false;

        } catch (Exception e) {
            log.error("Error verifying MFA code for user {}: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * Verify TOTP MFA code
     */
    private boolean verifyTotpMfa(UserMFA userMFA, String code) {
        try {
            int numericCode = Integer.parseInt(code);
            return googleAuthenticator.authorize(userMFA.getSecretKey(), numericCode);
        } catch (NumberFormatException e) {
            log.warn("Invalid TOTP code format for user {}: {}", userMFA.getUserId(), e.getMessage());
            return false;
        }
    }

    /**
     * Verify Azure Entra ID MFA code
     */
    private boolean verifyAzureEntraIdMfa(UserMFA userMFA, String code) {
        try {
            // Use the secret key as user principal name for Azure MFA
            String userPrincipalName = userMFA.getSecretKey();
            return azureMfaService.verifyAzureMfa(userPrincipalName, code);
        } catch (Exception e) {
            log.error("Error verifying Azure Entra ID MFA for user {}: {}", userMFA.getUserId(), e.getMessage());
            return false;
        }
    }

    /**
     * Verify MFA code (request object version)
     */
    public LoginResponse verifyMfaCode(MfaVerificationRequest request, HttpServletRequest httpRequest) {
        // Get user ID from session ID (simplified implementation)
        UUID userId = UUID.fromString(request.getSessionId()); // In reality, session management is needed
        
        boolean isValid = verifyMfaCode(userId, request.getMfaCode());
        
        if (!isValid) {
            return LoginResponse.error("Invalid MFA code");
        }
        
        // Token generation after successful MFA verification (simplified)
        // In actual implementation, integration with authentication service is necessary
        return LoginResponse.success(null, null, null);
    }

    /**
     * Set up MFA for a user with QR code generation or Azure Entra ID setup
     */
    public MfaSetupResponse setupMfa(MfaSetupRequest request) {
        try {
            UUID userId = request.getUserIdAsUUID();
            String accountName = request.getAccountName() != null ? request.getAccountName() : "user@skishop.com";
            String issuer = request.getIssuer() != null ? request.getIssuer() : DEFAULT_ISSUER;
            MfaType mfaType = request.getMfaTypeEnum();
            
            // Generate recovery codes
            List<String> recoveryCodes = generateRecoveryCodes();
            
            if (mfaType == MfaType.AZURE_ENTRA_ID) {
                return setupAzureEntraIdMfa(userId, accountName, recoveryCodes);
            } else {
                return setupTotpMfa(userId, accountName, issuer, recoveryCodes);
            }
            
        } catch (Exception e) {
            log.error("Error setting up MFA for user {}: {}", request.getUserId(), e.getMessage());
            throw new RuntimeException("Failed to setup MFA");
        }
    }

    /**
     * Set up TOTP MFA
     */
    private MfaSetupResponse setupTotpMfa(UUID userId, String accountName, String issuer, List<String> recoveryCodes) {
        // Generate secret key using GoogleAuth
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secretKey = key.getKey();
        
        // Create or update UserMFA entity
        UserMFA userMFA = UserMFA.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .mfaType("TOTP")
            .secretKey(secretKey)
            .backupCodes(recoveryCodes)
            .isEnabled(true)
            .build();
        
        entityManager.persist(userMFA);
        
        // Generate QR code URL and Base64 image
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, accountName, key);
        String qrCodeBase64 = generateQRCodeBase64(qrCodeUrl);
        
        log.info("TOTP MFA setup completed for user: {}", userId);
        
        return MfaSetupResponse.success(
            secretKey, 
            qrCodeUrl, 
            qrCodeBase64, 
            recoveryCodes, 
            issuer, 
            accountName
        );
    }

    /**
     * Set up Azure Entra ID MFA
     */
    private MfaSetupResponse setupAzureEntraIdMfa(UUID userId, String accountName, List<String> recoveryCodes) {
        // Setup Azure Entra ID MFA
        AzureMfaService.AzureMfaSetupResult azureResult = azureMfaService.setupAzureMfa(accountName);
        
        if (!azureResult.isSuccess()) {
            throw new RuntimeException("Failed to setup Azure Entra ID MFA: " + azureResult.getErrorMessage());
        }
        
        // Create or update UserMFA entity
        UserMFA userMFA = UserMFA.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .mfaType("AZURE_ENTRA_ID")
            .secretKey(accountName) // Store user principal name
            .backupCodes(recoveryCodes)
            .isEnabled(true)
            .build();
        
        entityManager.persist(userMFA);
        
        log.info("Azure Entra ID MFA setup completed for user: {}", userId);
        
        return MfaSetupResponse.azureSuccess(
            azureResult.getSetupUrl(),
            azureResult.getPollingKey(),
            recoveryCodes,
            accountName
        );
    }

    /**
     * Disable MFA
     */
    public void disableMfa(UUID userId) {
        try {
            UserMFA userMFA = entityManager.createQuery(
                "SELECT um FROM UserMFA um WHERE um.user.id = :userId", 
                UserMFA.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .orElse(null);

            if (userMFA != null) {
                userMFA.setIsEnabled(false);
                entityManager.merge(userMFA);
                log.info("MFA disabled for user: {}", userId);
            }
            
        } catch (Exception e) {
            log.error("Error disabling MFA for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to disable MFA");
        }
    }
    
    /**
     * Generate recovery codes
     */
    private List<String> generateRecoveryCodes() {
        List<String> recoveryCodes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < RECOVERY_CODES_COUNT; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < RECOVERY_CODE_LENGTH; j++) {
                sb.append(random.nextInt(10));
            }
            recoveryCodes.add(sb.toString());
        }
        
        return recoveryCodes;
    }
    
    /**
     * Generate QR code as Base64 encoded image
     */
    private String generateQRCodeBase64(String qrCodeUrl) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUrl, BarcodeFormat.QR_CODE, 200, 200);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            
            byte[] qrCodeBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(qrCodeBytes);
            
        } catch (Exception e) {
            log.error("Error generating QR code: {}", e.getMessage());
            throw new RuntimeException("Failed to generate QR code");
        }
    }
    
    /**
     * Check if the provided code is a recovery code
     */
    private boolean isRecoveryCode(UserMFA userMFA, String code) {
        if (userMFA.getBackupCodes() == null || code == null) {
            return false;
        }
        
        // Check if code matches any recovery code
        boolean isValid = userMFA.getBackupCodes().contains(code);
        
        if (isValid) {
            // Remove used recovery code
            userMFA.getBackupCodes().remove(code);
            entityManager.merge(userMFA);
        }
        
        return isValid;
    }
    
    /**
     * Generate new recovery codes for existing MFA setup
     */
    public List<String> regenerateRecoveryCodes(UUID userId) {
        try {
            UserMFA userMFA = entityManager.createQuery(
                "SELECT um FROM UserMFA um WHERE um.user.id = :userId AND um.isEnabled = true", 
                UserMFA.class)
                .setParameter("userId", userId)
                .getResultStream()
                .findFirst()
                .orElse(null);

            if (userMFA == null) {
                throw new RuntimeException("MFA not enabled for user");
            }
            
            List<String> newRecoveryCodes = generateRecoveryCodes();
            userMFA.setBackupCodes(newRecoveryCodes);
            entityManager.merge(userMFA);
            
            log.info("Recovery codes regenerated for user: {}", userId);
            return newRecoveryCodes;
            
        } catch (Exception e) {
            log.error("Error regenerating recovery codes for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to regenerate recovery codes");
        }
    }
}
