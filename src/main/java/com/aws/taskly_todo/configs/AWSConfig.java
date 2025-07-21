package com.aws.taskly_todo.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class AWSConfig {
    
    @Bean
    public DynamoDbClient dynamoDbClient(@Value( "${aws.region}" ) String region) {
        return DynamoDbClient.builder()
                .region( Region.of(region))
                .credentialsProvider( DefaultCredentialsProvider.create())
                .build();
    }
}
