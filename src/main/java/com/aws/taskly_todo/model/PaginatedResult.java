package com.aws.taskly_todo.model;

import java.util.List;

public record PaginatedResult<T>(
        List<T> items,
        String lastEvaluatedKey ) {
}
