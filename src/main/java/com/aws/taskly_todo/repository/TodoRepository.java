package com.aws.taskly_todo.repository;

import com.aws.taskly_todo.model.PaginatedResult;
import com.aws.taskly_todo.model.Status;
import com.aws.taskly_todo.model.TodoItem;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class TodoRepository {
    
    public static final String TODOS = "TODOS";
    public static final String CREATED_AT_INDEX = "CreatedAtIndex";
    public static final String SORT_KEY_SORT_KEY = "sortKey = :sortKey";
    private final DynamoDbClient dynamoDbClient;
    private final String tableName = "TasklyTodoItems";
    
    public TodoRepository (DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }
    
    public void save (String title, String description, String dueDate) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put( "id", AttributeValue.builder().s( UUID.randomUUID().toString() ).build() );
        item.put( "title", AttributeValue.builder().s( title ).build() );
        item.put( "description", AttributeValue.builder().s( description ).build() );
        item.put( "status", AttributeValue.builder().s( Status.PENDING.name() ).build() );
        item.put( "dueDate", AttributeValue.builder().s( dueDate ).build() );
        item.put( "createdAt", AttributeValue.builder().s( Instant.now().toString() ).build() );
        item.put( "sortKey", AttributeValue.builder().s( TODOS ).build() );
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName( tableName )
                .item( item )
                .build();
        
        dynamoDbClient.putItem( request );
    }
    
    public PaginatedResult<TodoItem> findAll (int limit, Map<String, AttributeValue> exclusiveStartKey) {
        QueryRequest.Builder queryBuilder = QueryRequest.builder()
                .tableName( tableName )
                .indexName( CREATED_AT_INDEX )
                .keyConditionExpression( SORT_KEY_SORT_KEY )
                .expressionAttributeValues( Map.of( ":sortKey", AttributeValue.builder().s( TODOS ).build() ) )
                .limit( limit )
                .scanIndexForward( false ); // Descending order
        
        return getTodoItemPaginatedResult( exclusiveStartKey, queryBuilder );
    }
    
    public Optional<TodoItem> findById (String id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put( "id", AttributeValue.builder().s( id ).build() );
        
        GetItemRequest request = GetItemRequest.builder()
                .tableName( tableName )
                .key( key )
                .build();
        
        Map<String, AttributeValue> item = dynamoDbClient.getItem( request ).item();
        
        if (item == null || item.isEmpty()) return Optional.empty();
        
        return Optional.of(mapToTodoItem(item));
    }
    
    public PaginatedResult<TodoItem> findByStatus(String status, int limit, Map<String, AttributeValue> exclusiveStartKey) {
        QueryRequest.Builder requestBuilder = QueryRequest.builder()
                .tableName(tableName)
                .indexName("StatusIndex")
                .keyConditionExpression("#status = :status")
                .expressionAttributeNames(Map.of("#status", "status"))
                .expressionAttributeValues(Map.of(":status", AttributeValue.builder().s(status).build()))
                .limit(limit)
                .scanIndexForward(false);
        
        if (exclusiveStartKey != null && !exclusiveStartKey.isEmpty()) {
            requestBuilder.exclusiveStartKey(exclusiveStartKey);
        }
        
        return getTodoItemPaginatedResult( exclusiveStartKey, requestBuilder );
    }
    
    public PaginatedResult<TodoItem> findByDueDate(String dueDate, int limit, Map<String, AttributeValue> exclusiveStartKey) {
        QueryRequest.Builder requestBuilder = QueryRequest.builder()
                .tableName(tableName)
                .indexName("DueDateIndex")
                .keyConditionExpression("dueDate = :dueDate")
                .expressionAttributeValues(Map.of(":dueDate", AttributeValue.builder().s(dueDate).build()))
                .limit(limit)
                .scanIndexForward(false);
        
        if (exclusiveStartKey != null && !exclusiveStartKey.isEmpty()) {
            requestBuilder.exclusiveStartKey(exclusiveStartKey);
        }
        
        return getTodoItemPaginatedResult( exclusiveStartKey, requestBuilder );
    }
    
    public PaginatedResult<TodoItem> findByStatusAndDueDate(String status, String dueDate, int limit, Map<String, AttributeValue> exclusiveStartKey) {
        QueryRequest.Builder requestBuilder = QueryRequest.builder()
                .tableName(tableName)
                .indexName("StatusIndex")
                .keyConditionExpression("#status = :status AND dueDate = :dueDate")
                .expressionAttributeNames(Map.of("#status", "status"))
                .expressionAttributeValues(Map.of(
                        ":status", AttributeValue.builder().s(status).build(),
                        ":dueDate", AttributeValue.builder().s(dueDate).build()
                ))
                .limit(limit)
                .scanIndexForward(false);
        
        return getTodoItemPaginatedResult( exclusiveStartKey, requestBuilder );
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
    
    private TodoItem mapToTodoItem(Map<String, AttributeValue> item) {
        return new TodoItem(
                item.get("id").s(),
                item.get("title").s(),
                item.get("description").s(),
                item.get("dueDate").s(),
                item.containsKey("status") ? item.get("status").s() : Status.PENDING.name(), // ✅ fallback
                item.get("createdAt").s(),
                item.containsKey("updatedAt") ? item.get("updatedAt").s() : null,            // optional field
                item.get("sortKey").s()
        );
    }
    
    private PaginatedResult<TodoItem> getTodoItemPaginatedResult (Map<String, AttributeValue> exclusiveStartKey, QueryRequest.Builder requestBuilder) {
        if (exclusiveStartKey != null && !exclusiveStartKey.isEmpty())
            requestBuilder.exclusiveStartKey( exclusiveStartKey );
        
        QueryResponse response = dynamoDbClient.query(requestBuilder.build());
        
        List<TodoItem> todos = response.items().stream()
                .map( this::mapToTodoItem )
                .collect( Collectors.toList() );
        
        // Handle encoding of LastEvaluatedKey
        String lastKeyEncoded = null;
        if (response.hasLastEvaluatedKey() && !response.lastEvaluatedKey().isEmpty()) {
            Map<String, AttributeValue> lastEvaluatedKey = response.lastEvaluatedKey();
            String keyString = lastEvaluatedKey.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue().s())
                    .collect(Collectors.joining(","));
            lastKeyEncoded = Base64.getEncoder().encodeToString(keyString.getBytes());
        }
        
        return new PaginatedResult<>(todos, lastKeyEncoded);
    }
}