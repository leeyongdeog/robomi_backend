package com.robomi.service;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;

public class S3Uploader {
    private static String aws_accessKey = "";
    private static String aws_secretKey = "";

    public static void uploadFile(String bucketName, String folderName, String keyName, MultipartFile file){
        Region region = Region.AP_NORTHEAST_2;
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(aws_accessKey, aws_secretKey);
        S3Client s3Client = S3Client.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();

        try{
            byte[] fileBytes = file.getBytes();
            String fk_name = folderName + "/" + keyName + ".jpg";
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fk_name)
                    .build();

            PutObjectResponse response = s3Client.putObject(objectRequest, RequestBody.fromBytes(fileBytes));
            System.out.println("File upload to S3 OK "+response);
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }
}
