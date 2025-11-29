package com.ammons.taskactivity.config;

import com.ammons.taskactivity.service.LocalFileStorageService;
import com.ammons.taskactivity.service.ReceiptStorageService;
import com.ammons.taskactivity.service.S3StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for receipt storage Switches between local and S3 storage based on configuration
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Configuration
public class StorageConfig {

    @Value("${storage.type:local}")
    private String storageType;

    @Bean
    public ReceiptStorageService receiptStorageService(
            @Value("${storage.local.path:c:/Task Activity/Receipts}") String localPath,
            @Value("${storage.s3.bucket:taskactivity-receipts-prod}") String s3Bucket,
            @Value("${storage.s3.region:us-east-1}") String s3Region) {

        if ("s3".equalsIgnoreCase(storageType)) {
            return new S3StorageService(s3Bucket, s3Region);
        } else {
            return new LocalFileStorageService(localPath);
        }
    }
}
