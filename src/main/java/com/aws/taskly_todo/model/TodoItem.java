package com.aws.taskly_todo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TodoItem {
    private String id;
    private String title;
    private String description;
    private String dueDate;
    private String status;
    private String createdAt;
    private String updatedAt;
}
