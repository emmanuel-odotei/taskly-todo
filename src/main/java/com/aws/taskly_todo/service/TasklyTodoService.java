package com.aws.taskly_todo.service;

import com.aws.taskly_todo.model.PaginatedResult;
import com.aws.taskly_todo.model.Status;
import com.aws.taskly_todo.model.TodoItem;
import com.aws.taskly_todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TasklyTodoService {
    private final TodoRepository todoRepository;
    
    public PaginatedResult<TodoItem> getTodos (String status, String dueDate, int limit, String lastKeyEncoded) {
        Map<String, AttributeValue> exclusiveStartKey = decodeLastKey( lastKeyEncoded );
        int pageSize = ( limit <= 0 ) ? 10 : limit;
        
        if ( status != null && !status.isBlank() && dueDate != null && !dueDate.isBlank() ) {
            return todoRepository.findByStatusAndDueDate( status, dueDate, pageSize, exclusiveStartKey );
        } else if ( status != null && !status.isBlank() && ( dueDate == null || dueDate.isBlank() ) ) {
            return todoRepository.findByStatus( status, pageSize, exclusiveStartKey );
        } else if ( dueDate != null && !dueDate.isBlank() ) {
            return todoRepository.findByDueDate( dueDate, pageSize, exclusiveStartKey );
        } else {
            return todoRepository.findAll( pageSize, exclusiveStartKey );
        }
    }
    
    public TodoItem getTodoItem (String id) {
        return todoRepository.findById( id ).orElseThrow( () -> new RuntimeException( "Todo item not found" ) );
    }
    
    public void createTodo (String title, String description, String dueDate) {
        try {
            todoRepository.save( title, description, dueDate );
        } catch ( Exception e ) {
            throw new RuntimeException( "Failed to create todo item" );
        }
    }
    
    public void deleteTodo (String id) {
        try {
            todoRepository.deleteById( id );
        } catch ( Exception e ) {
            throw new RuntimeException( "Failed to delete todo item" );
        }
    }
    
    public void updateTodo (String id, String title, String description, String dueDate, Status status) {
        try {
            todoRepository.updateTodo( id, title, description, status, dueDate );
        } catch ( Exception e ) {
            throw new RuntimeException( "Failed to update todo item" );
        }
    }
    
    private Map<String, AttributeValue> decodeLastKey (String encodedKey) {
        if ( encodedKey == null || encodedKey.isEmpty() ) return Collections.emptyMap();
        
        try {
            byte[] bytes = Base64.getDecoder().decode( encodedKey );
            String keyString = new String( bytes );
            Map<String, AttributeValue> keyMap = new HashMap<>();
            
            for ( String entry : keyString.split( "," ) ) {
                String[] kv = entry.split( "=" );
                keyMap.put( kv[ 0 ], AttributeValue.builder().s( kv[ 1 ] ).build() );
            }
            
            return keyMap;
        } catch ( Exception e ) {
            return Collections.emptyMap();
        }
    }
}
