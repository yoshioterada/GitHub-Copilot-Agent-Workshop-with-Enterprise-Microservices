package com.skishop.auth.dto;

/**
 * MFA Type Enumeration
 * 
 * Defines the supported MFA types
 */
public enum MfaType {
    TOTP("TOTP"),
    AZURE_ENTRA_ID("AZURE_ENTRA_ID");
    
    private final String value;
    
    MfaType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public static MfaType fromString(String value) {
        for (MfaType type : MfaType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MFA type: " + value);
    }
}