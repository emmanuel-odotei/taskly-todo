package com.aws.taskly_todo.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
public class AppConfig {

    /**
     * Provides a DynamoDB client for the region specified in
     * {@code application.properties} as {@code aws.region}.
     *
     * @param region the region in which the DynamoDB client should be created
     * @return a DynamoDB client for the specified region
     */
    @Bean
    public DynamoDbClient dynamoDbClient(@Value( "${aws.region}" ) String region) {
        return DynamoDbClient.builder()
                .region( Region.of(region))
                .credentialsProvider( DefaultCredentialsProvider.create())
                .build();
    }
    
    /**
     * Creates a Spring {@link FilterRegistrationBean} for the
     * {@link HiddenHttpMethodFilter}, which is used to support
     * HTTP methods other than GET and POST, such as PUT and DELETE. For thymeleaf form html template
     *
     * @return a {@link FilterRegistrationBean} for the
     * {@link HiddenHttpMethodFilter}
     */
    @Bean
    public FilterRegistrationBean<HiddenHttpMethodFilter> hiddenHttpMethodFilter() {
        FilterRegistrationBean<HiddenHttpMethodFilter> filterRegistrationBean = new FilterRegistrationBean<>(new HiddenHttpMethodFilter());
        filterRegistrationBean.setOrder(1);
        return filterRegistrationBean;
    }
}
