# Azure Entra ID MFA Integration

This document describes the Azure Entra ID MFA integration implemented in the Authentication Service.

## Overview

The Authentication Service now supports two types of Multi-Factor Authentication (MFA):
1. **TOTP (Time-based One-Time Password)** - Using Google Authenticator compatible apps
2. **Azure Entra ID MFA** - Using Microsoft's enterprise MFA solution

## Azure Entra ID MFA Features

### Setup Process
- Users can set up Azure Entra ID MFA by specifying `mfaType: "AZURE_ENTRA_ID"` in the setup request
- The service generates a setup URL directing users to Microsoft's security info page
- Recovery codes are generated for backup access

### Verification Methods
The Azure MFA implementation supports multiple verification methods:
- **6-digit TOTP codes** from Microsoft Authenticator app
- **Push notifications** (represented as "APPROVED")
- **Phone call verification** (represented as "VERIFIED")

### Configuration
Azure MFA configuration is handled through the existing Azure Entra ID settings in `application.yml`:

```yaml
spring:
  cloud:
    azure:
      active-directory:
        enabled: true
        profile:
          tenant-id: ${AZURE_TENANT_ID:your-tenant-id-here}
        credential:
          client-id: ${AZURE_CLIENT_ID:your-client-id-here}
          client-secret: ${AZURE_CLIENT_SECRET:your-client-secret-here}
```

## API Usage

### Setup Azure Entra ID MFA
```json
POST /api/mfa/setup
{
  "userId": "user-uuid",
  "accountName": "user@company.com",
  "mfaType": "AZURE_ENTRA_ID"
}
```

Response:
```json
{
  "azureAuthenticatorSetupUrl": "https://mysignins.microsoft.com/security-info?upn=user@company.com",
  "azurePollingKey": "polling-key-uuid",
  "recoveryCodes": ["12345678", "87654321", ...],
  "accountName": "user@company.com",
  "mfaType": "AZURE_ENTRA_ID"
}
```

### Setup TOTP MFA (existing functionality)
```json
POST /api/mfa/setup
{
  "userId": "user-uuid",
  "accountName": "user@company.com",
  "mfaType": "TOTP"
}
```

### Verify MFA Code
Both Azure and TOTP MFA use the same verification endpoint:
```json
POST /api/mfa/verify
{
  "userId": "user-uuid",
  "code": "123456"  // or "APPROVED" for Azure push notifications
}
```

## Implementation Notes

### Production Considerations
The current implementation is a demonstration/foundation that would need to be enhanced for production use:

1. **Microsoft Graph API Integration**: Replace placeholder verification with actual Graph API calls
2. **Authentication Flow**: Implement proper OAuth 2.0 flow for Azure AD B2C
3. **Error Handling**: Add comprehensive error handling for Azure AD responses
4. **Polling Mechanism**: Implement polling for push notification results
5. **Device Management**: Add device registration and management features

### Security Features
- Recovery codes are securely generated using `SecureRandom`
- MFA types are validated using enum constraints
- User principal names are stored securely for Azure MFA verification
- All Azure MFA operations are logged for audit purposes

## Service Architecture

### Components
- `MfaService` - Main service handling both TOTP and Azure MFA
- `AzureMfaService` - Dedicated service for Azure Entra ID MFA operations
- `MfaType` - Enum defining supported MFA types
- `AzureMfaConfig` - Configuration for Azure MFA beans
- Enhanced DTOs supporting both MFA types

### Database Schema
The `UserMFA` entity supports both MFA types:
- `mfaType`: Either "TOTP" or "AZURE_ENTRA_ID"
- `secretKey`: TOTP secret for TOTP, user principal name for Azure MFA
- `backupCodes`: Recovery codes for both types

## Testing

Comprehensive test coverage includes:
- Azure MFA setup and verification
- TOTP MFA setup and verification
- MFA type validation
- Error handling scenarios
- Mock integration testing

Run tests with:
```bash
mvn test -Dtest=*MfaTest
```