package com.skishop.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MFA Setup Response DTO
 * 
 * Response containing MFA setup information including QR code and recovery codes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MfaSetupResponse {
    
    private String secretKey;
    private String qrCodeUrl;
    private String qrCodeBase64;
    private List<String> recoveryCodes;
    private String issuer;
    private String accountName;
    private String mfaType;
    
    // Azure Entra ID specific fields
    private String azureAuthenticatorSetupUrl;
    private String azurePollingKey;
    
    public static MfaSetupResponse success(String secretKey, String qrCodeUrl, String qrCodeBase64, List<String> recoveryCodes, String issuer, String accountName) {
        return MfaSetupResponse.builder()
                .secretKey(secretKey)
                .qrCodeUrl(qrCodeUrl)
                .qrCodeBase64(qrCodeBase64)
                .recoveryCodes(recoveryCodes)
                .issuer(issuer)
                .accountName(accountName)
                .mfaType("TOTP")
                .build();
    }
    
    public static MfaSetupResponse azureSuccess(String azureAuthenticatorSetupUrl, String azurePollingKey, List<String> recoveryCodes, String accountName) {
        return MfaSetupResponse.builder()
                .azureAuthenticatorSetupUrl(azureAuthenticatorSetupUrl)
                .azurePollingKey(azurePollingKey)
                .recoveryCodes(recoveryCodes)
                .accountName(accountName)
                .mfaType("AZURE_ENTRA_ID")
                .build();
    }
    
    // Manual getter methods since Lombok may not be working properly
    public String getSecretKey() { return secretKey; }
    public String getQrCodeUrl() { return qrCodeUrl; }
    public String getQrCodeBase64() { return qrCodeBase64; }
    public List<String> getRecoveryCodes() { return recoveryCodes; }
    public String getIssuer() { return issuer; }
    public String getAccountName() { return accountName; }
    public String getMfaType() { return mfaType; }
    public String getAzureAuthenticatorSetupUrl() { return azureAuthenticatorSetupUrl; }
    public String getAzurePollingKey() { return azurePollingKey; }
}