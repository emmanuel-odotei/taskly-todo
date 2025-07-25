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
    private String sortKey;
    
    /**
     * Returns a Bootstrap class name corresponding to the status of the to-do item
     * to style the status field in the UI.
     *
     * @return a CSS class name to style the status field in the UI.
     */
    public String getStatusStyleClass() {
        return switch (status) {
            case "PENDING" -> "bg-light text-secondary";
            case "ONGOING" -> "bg-warning-subtle text-dark";
            case "CANCELLED" -> "bg-danger-subtle text-dark";
            case "COMPLETED"-> "bg-success-subtle text-dark";
            default -> "";
        };
    }
}