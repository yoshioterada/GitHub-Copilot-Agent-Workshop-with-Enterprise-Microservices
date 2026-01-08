package com.skishop.auth.config;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MFA Configuration
 * 
 * Configuration for Multi-Factor Authentication components
 */
@Configuration
public class MfaConfig {
    
    @Bean
    public GoogleAuthenticator googleAuthenticator() {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
            .setTimeStepSizeInMillis(30000) // 30 seconds time window
            .setWindowSize(3) // Allow 3 time windows for clock skew
            .setCodeDigits(6) // 6-digit codes
            .build();
            
        return new GoogleAuthenticator(config);
    }
}