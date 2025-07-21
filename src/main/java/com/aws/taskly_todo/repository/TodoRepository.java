package com.aws.taskly_todo.repository;

import com.aws.taskly_todo.model.PaginatedResult;
import com.aws.taskly_todo.model.Status;
import com.aws.taskly_todo.model.TodoItem;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class TodoRepository {
    
    private final DynamoDbClient dynamoDbClient;
    private final String tableName = "TasklyTodoItems";
    
    public TodoRepository (DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }
    
    public void save (TodoItem todo) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put( "id", AttributeValue.builder().s( todo.getId() ).build() );
        item.put( "title", AttributeValue.builder().s( todo.getTitle() ).build() );
        item.put( "description", AttributeValue.builder().s( todo.getDescription() ).build() );
        item.put( "status", AttributeValue.builder().s( Status.PENDING.name() ).build() );
        item.put( "dueDate", AttributeValue.builder().s( todo.getDueDate() ).build() );
        item.put( "createdAt", AttributeValue.builder().s( Instant.now().toString() ).build() );
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName( tableName )
                .item( item )
                .build();
        
        dynamoDbClient.putItem( request );
    }
    
    public PaginatedResult<TodoItem> findAll (int limit, Map<String, AttributeValue> exclusiveStartKey) {
        ScanRequest.Builder scanRequestBuilder = ScanRequest.builder()
                .tableName( tableName )
                .limit( limit );
        
        if ( exclusiveStartKey != null && !exclusiveStartKey.isEmpty() )
            scanRequestBuilder.exclusiveStartKey( exclusiveStartKey );
        
        ScanResponse response = dynamoDbClient.scan( scanRequestBuilder.build() );
        
        List<TodoItem> todos = response.items().stream()
                .map( this::mapToTodoItem )
                .sorted( Comparator.comparing( TodoItem::getCreatedAt ).reversed() )
                .collect( Collectors.toList() );
        
        return new PaginatedResult<>( todos, response.lastEvaluatedKey() );
    }
    
    public TodoItem findById(String id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("id", AttributeValue.builder().s(id).build());
        
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();
        
        Map<String, AttributeValue> item = dynamoDbClient.getItem(request).item();
        
        if (item == null || item.isEmpty()) {
            return null;
        }
        return mapToTodoItem(item);
    }
    
    public void deleteById (String id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put( "id", AttributeValue.builder().s( id ).build() );
        
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName( tableName )
                .key( key )
                .build();
        
        dynamoDbClient.deleteItem( request );
    }
    
    public void updateTodo (String id, String title, String description, Status status, String dueDate) {
        Map<String, AttributeValue> key = Map.of(
                "id", AttributeValue.builder().s( id ).build()
        );
        
        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        
        if ( title != null ) {
            updates.put( "title", AttributeValueUpdate.builder()
                    .value( AttributeValue.builder().s( title ).build() )
                    .action( AttributeAction.PUT )
                    .build() );
        }
        
        if ( description != null ) {
            updates.put( "description", AttributeValueUpdate.builder()
                    .value( AttributeValue.builder().s( description ).build() )
                    .action( AttributeAction.PUT )
                    .build() );
        }
        
        if ( status != null ) {
            updates.put( "status", AttributeValueUpdate.builder()
                    .value( AttributeValue.builder().s( status.name() ).build() )
                    .action( AttributeAction.PUT )
                    .build() );
        }
        
        if ( dueDate != null ) {
            updates.put( "dueDate", AttributeValueUpdate.builder()
                    .value( AttributeValue.builder().s( dueDate ).build() )
                    .action( AttributeAction.PUT )
                    .build() );
        }
        
        // Always update the updatedAt timestamp to current time
        updates.put( "updatedAt", AttributeValueUpdate.builder()
                .value( AttributeValue.builder().s( Instant.now().toString() ).build() )
                .action( AttributeAction.PUT )
                .build() );
        
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName( tableName )
                .key( key )
                .attributeUpdates( updates )
                .build();
        
        dynamoDbClient.updateItem( request );
    }
    
    private TodoItem mapToTodoItem (Map<String, AttributeValue> item) {
        return new TodoItem(
                item.get( "id" ).s(),
                item.get( "title" ).s(),
                item.get( "description" ).s(),
                item.get( "status" ).s(),
                item.get( "dueDate" ).s(),
                item.get( "createdAt" ).s(),
                item.get( "updatedAt" ).s()
        );
    }
}