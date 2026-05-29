package com.iare.placementportal.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudinaryConfig.class);

    @Bean
    public Cloudinary cloudinary(@Value("${cloudinary.cloud-name:}") String cloudName,
                                 @Value("${cloudinary.api-key:}") String apiKey,
                                 @Value("${cloudinary.api-secret:}") String apiSecret) {
        boolean cloudNamePresent = cloudName != null && !cloudName.isBlank();
        boolean apiKeyPresent = apiKey != null && !apiKey.isBlank();
        boolean apiSecretPresent = apiSecret != null && !apiSecret.isBlank();

        if (cloudNamePresent && apiKeyPresent && apiSecretPresent) {
            LOGGER.info("Cloudinary configuration loaded for preparation resource uploads.");
        } else {
            LOGGER.warn("Cloudinary configuration is incomplete. Uploads will fail until cloud-name, api-key, and api-secret are provided.");
        }

        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}
