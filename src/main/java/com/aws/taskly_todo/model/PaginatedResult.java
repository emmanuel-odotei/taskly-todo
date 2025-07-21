package com.aws.taskly_todo.model;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;

public record PaginatedResult<T>(
        List<T> items,
        Map<String, AttributeValue> lastEvaluatedKey ) {
}
