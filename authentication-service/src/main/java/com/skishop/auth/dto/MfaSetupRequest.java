package com.skishop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

/**
 * MFA Setup Request DTO
 * 
 * Request for setting up MFA for a user
 */
@Data
public class MfaSetupRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    private String accountName;
    private String issuer;
    
    // MFA type (TOTP or AZURE_ENTRA_ID)
    private String mfaType = "TOTP"; // Default to TOTP for backward compatibility
    
    // Manual getter methods since Lombok may not be working properly
    public String getUserId() { return userId; }
    public String getAccountName() { return accountName; }
    public String getIssuer() { return issuer; }
    public String getMfaType() { return mfaType; }
    
    public UUID getUserIdAsUUID() {
        return UUID.fromString(userId);
    }
    
    public MfaType getMfaTypeEnum() {
        return MfaType.fromString(mfaType);
    }
}