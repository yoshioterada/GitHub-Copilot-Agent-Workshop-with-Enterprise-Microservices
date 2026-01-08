package com.skishop.auth.service.azure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for AzureMfaService
 */
@ExtendWith(MockitoExtension.class)
class AzureMfaServiceTest {

    @InjectMocks
    private AzureMfaService azureMfaService;

    @Test
    void testSetupAzureMfa() {
        // Arrange
        ReflectionTestUtils.setField(azureMfaService, "tenantId", "test-tenant-id");
        String userPrincipalName = "user@skishop.com";

        // Act
        AzureMfaService.AzureMfaSetupResult result = azureMfaService.setupAzureMfa(userPrincipalName);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(userPrincipalName, result.getUserPrincipalName());
        assertNotNull(result.getSetupUrl());
        assertTrue(result.getSetupUrl().contains(userPrincipalName));
        assertNotNull(result.getPollingKey());
    }

    @Test
    void testVerifyAzureMfa() {
        // Arrange
        String userPrincipalName = "user@skishop.com";

        // Test valid 6-digit code
        assertTrue(azureMfaService.verifyAzureMfa(userPrincipalName, "123456"));

        // Test push notification approval
        assertTrue(azureMfaService.verifyAzureMfa(userPrincipalName, "APPROVED"));

        // Test phone call verification
        assertTrue(azureMfaService.verifyAzureMfa(userPrincipalName, "VERIFIED"));

        // Test invalid codes
        assertFalse(azureMfaService.verifyAzureMfa(userPrincipalName, "000000"));
        assertFalse(azureMfaService.verifyAzureMfa(userPrincipalName, "invalid"));
        assertFalse(azureMfaService.verifyAzureMfa(userPrincipalName, null));
        assertFalse(azureMfaService.verifyAzureMfa(userPrincipalName, ""));
    }

    @Test
    void testIsAzureMfaEnabled() {
        // Arrange
        String userPrincipalName = "user@skishop.com";

        // Act & Assert
        assertTrue(azureMfaService.isAzureMfaEnabled(userPrincipalName));
    }

    @Test
    void testDisableAzureMfa() {
        // Arrange
        String userPrincipalName = "user@skishop.com";

        // Act & Assert
        assertTrue(azureMfaService.disableAzureMfa(userPrincipalName));
    }
}