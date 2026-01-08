package com.skishop.auth.service;

import com.skishop.auth.dto.MfaSetupRequest;
import com.skishop.auth.dto.MfaSetupResponse;
import com.skishop.auth.dto.MfaType;
import com.skishop.auth.entity.UserMFA;
import com.skishop.auth.service.azure.AzureMfaService;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test class for MfaService Azure integration
 */
@ExtendWith(MockitoExtension.class)
class MfaServiceAzureTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private GoogleAuthenticator googleAuthenticator;

    @Mock
    private AzureMfaService azureMfaService;

    @InjectMocks
    private MfaService mfaService;

    @Test
    void testSetupAzureEntraIdMfa() {
        // Arrange
        MfaSetupRequest request = new MfaSetupRequest();
        request.setUserId(UUID.randomUUID().toString());
        request.setAccountName("user@skishop.com");
        request.setMfaType("AZURE_ENTRA_ID");

        AzureMfaService.AzureMfaSetupResult azureResult = AzureMfaService.AzureMfaSetupResult.builder()
                .setupUrl("https://mysignins.microsoft.com/security-info?upn=user@skishop.com")
                .pollingKey("test-polling-key")
                .userPrincipalName("user@skishop.com")
                .success(true)
                .build();

        when(azureMfaService.setupAzureMfa(anyString())).thenReturn(azureResult);
        doNothing().when(entityManager).persist(any(UserMFA.class));

        // Act
        MfaSetupResponse response = mfaService.setupMfa(request);

        // Assert
        assertNotNull(response);
        assertEquals("AZURE_ENTRA_ID", response.getMfaType());
        assertEquals("https://mysignins.microsoft.com/security-info?upn=user@skishop.com", response.getAzureAuthenticatorSetupUrl());
        assertEquals("test-polling-key", response.getAzurePollingKey());
        assertEquals("user@skishop.com", response.getAccountName());
        assertNotNull(response.getRecoveryCodes());
        assertEquals(10, response.getRecoveryCodes().size());

        verify(azureMfaService).setupAzureMfa("user@skishop.com");
        verify(entityManager).persist(any(UserMFA.class));
    }

    @Test
    void testSetupTotpMfa() {
        // Arrange
        MfaSetupRequest request = new MfaSetupRequest();
        request.setUserId(UUID.randomUUID().toString());
        request.setAccountName("user@skishop.com");
        request.setMfaType("TOTP");

        when(googleAuthenticator.createCredentials()).thenReturn(createMockGoogleAuthKey());
        doNothing().when(entityManager).persist(any(UserMFA.class));

        // Act
        MfaSetupResponse response = mfaService.setupMfa(request);

        // Assert
        assertNotNull(response);
        assertEquals("TOTP", response.getMfaType());
        assertNotNull(response.getSecretKey());
        assertNotNull(response.getQrCodeUrl());
        assertNotNull(response.getQrCodeBase64());
        assertEquals("user@skishop.com", response.getAccountName());
        assertNotNull(response.getRecoveryCodes());
        assertEquals(10, response.getRecoveryCodes().size());

        verify(googleAuthenticator).createCredentials();
        verify(entityManager).persist(any(UserMFA.class));
    }

    @Test
    void testMfaTypeEnum() {
        // Test MfaType enum functionality
        assertEquals("TOTP", MfaType.TOTP.getValue());
        assertEquals("AZURE_ENTRA_ID", MfaType.AZURE_ENTRA_ID.getValue());

        assertEquals(MfaType.TOTP, MfaType.fromString("TOTP"));
        assertEquals(MfaType.AZURE_ENTRA_ID, MfaType.fromString("AZURE_ENTRA_ID"));

        assertThrows(IllegalArgumentException.class, () -> MfaType.fromString("INVALID"));
    }

    private com.warrenstrange.googleauth.GoogleAuthenticatorKey createMockGoogleAuthKey() {
        // Create a mock GoogleAuthenticatorKey
        return new com.warrenstrange.googleauth.GoogleAuthenticatorKey.Builder("TESTSECRETKEY123").build();
    }
}