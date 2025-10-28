package at.technikum.swen3.worker.service;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;

public class S3Service {
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final MinioClient minioClient;
    private final String bucketName;

    public S3Service(String minioUrl, String accessKey, String secretKey, String bucketName) {
        this.bucketName = bucketName;
        this.minioClient = MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
        
        logger.info("S3Service initialized with URL: {}, bucket: {}", minioUrl, bucketName);
    }

    public InputStream downloadFile(String s3Key) {
        try {
            logger.info("Downloading file from S3 with key: {}", s3Key);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(s3Key)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Error downloading file from S3: {}", s3Key, e);
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    public StatObjectResponse getObjectMetadata(String s3Key) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(s3Key)
                            .build()
            );
        } catch (Exception e) {
            logger.error("Error getting object metadata from S3: {}", s3Key, e);
            throw new RuntimeException("Failed to get object metadata from S3", e);
        }
    }
}
