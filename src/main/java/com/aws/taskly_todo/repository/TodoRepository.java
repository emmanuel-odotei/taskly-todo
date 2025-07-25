package com.aws.taskly_todo.repository;

import com.aws.taskly_todo.model.PaginatedResult;
import com.aws.taskly_todo.model.Status;
import com.aws.taskly_todo.model.TodoItem;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class TodoRepository {
    
    public static final String TODOS = "TODOS";
    public static final String CREATED_AT_INDEX = "CreatedAtIndex";
    public static final String SORT_KEY_SORT_KEY = "sortKey = :sortKey";
    public static final String DUE_DATE_INDEX = "DueDateIndex";
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String STATUS = "status";
    public static final String DUE_DATE = "dueDate";
    public static final String CREATED_AT = "createdAt";
    public static final String SORT_KEY = "sortKey";
    public static final String UPDATED_AT = "updatedAt";
    private static final String tableName = "TasklyTodoItems";
    private final DynamoDbClient dynamoDbClient;
    
    public TodoRepository (DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
    }
    
    /**
     * Saves a new to-do item with the given title, description and due date.
     *
     * @param title       The title of the to-do item.
     * @param description The description of the to-do item.
     * @param dueDate     The due date of the to-do item.
     */
    public void save (String title, String description, String dueDate) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put( ID, AttributeValue.builder().s( UUID.randomUUID().toString() ).build() );
        item.put( TITLE, AttributeValue.builder().s( title ).build() );
        item.put( DESCRIPTION, AttributeValue.builder().s( description ).build() );
        item.put( STATUS, AttributeValue.builder().s( Status.PENDING.name() ).build() );
        item.put( DUE_DATE, AttributeValue.builder().s( dueDate ).build() );
        item.put( CREATED_AT, AttributeValue.builder().s( LocalDateTime.now().toString() ).build() );
        item.put( SORT_KEY, AttributeValue.builder().s( TODOS ).build() );
        
        PutItemRequest request = PutItemRequest.builder()
                .tableName( tableName )
                .item( item )
                .build();
        
        dynamoDbClient.putItem( request );
    }
    
    /**
     * Returns a paginated result of all to-do items in dueDate today firs and descending order (newest first).
     *
     * @param limit             the maximum number of items to return
     * @param exclusiveStartKey the key from the previous query to start from.
     *                          If null, the query starts from the beginning.
     * @return the paginated result, containing the items and the last key.
     */
    public PaginatedResult<TodoItem> findAll (int limit, Map<String, AttributeValue> exclusiveStartKey) {
        QueryRequest.Builder queryBuilder = QueryRequest.builder()
                .tableName( tableName )
                .indexName( CREATED_AT_INDEX )
                .keyConditionExpression( SORT_KEY_SORT_KEY )
                .expressionAttributeValues( Map.of( ":sortKey", AttributeValue.builder().s( TODOS ).build() ) )
                .limit( limit )
                .scanIndexForward( true ); // or true for ascending
        
        return getTodoItemPaginatedResult( exclusiveStartKey, queryBuilder );
    }
    
    /**
     * Retrieves a to-do item by its ID.
     *
     * @param id The ID of the to-do item to retrieve.
     * @return An optional containing the to-do item if found, or empty if not.
     */
    public Optional<TodoItem> findById (String id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put( ID, AttributeValue.builder().s( id ).build() );
        
        GetItemRequest request = GetItemRequest.builder()
                .tableName( tableName )
                .key( key )
                .build();
        
        Map<String, AttributeValue> item = dynamoDbClient.getItem( request ).item();
        
        if ( item == null || item.isEmpty() ) return Optional.empty();
        
        return Optional.of( mapToTodoItem( item ) );
    }
    
    /**
     * Retrieves a paginated list of to-do items filtered by status.
     *
     * @param status            The status of the to-do items to filter by.
     * @param limit             The maximum number of items to return.
     * @param exclusiveStartKey The key from the previous query to start from.
     *                          If null, the query starts from the beginning.
     * @return A paginated result containing the filtered to-do items and the last key.
     */
    public PaginatedResult<TodoItem> findByStatus (String status, int limit, Map<String, AttributeValue> exclusiveStartKey) {
        QueryRequest.Builder requestBuilder = QueryRequest.builder()
                .tableName( tableName )
                .indexName( "StatusIndex" )
                .keyConditionExpression( "#status = :status" )
                .expressionAttributeNames( Map.of( "#status", STATUS ) )
                .expressionAttributeValues( Map.of( ":status", AttributeValue.builder().s( status ).build() ) )
                .limit( limit )
                .scanIndexForward( false );
        
        if ( exclusiveStartKey != null && !exclusiveStartKey.isEmpty() ) {
            requestBuilder.exclusiveStartKey( exclusiveStartKey );
        }
        
        return getTodoItemPaginatedResult( exclusiveStartKey, requestBuilder );
    }
    
    /**
     * Retrieves a paginated list of to-do items filtered by due date.
     *
     * @param dueDate           The due date of the to-do items to filter by.
     * @param limit             The maximum number of items to return.
     * @param exclusiveStartKey The key from the previous query to start from.
     *                          If null, the query starts from the beginning.
     * @return A paginated result containing the filtered to-do items and the last key.
     */
    public PaginatedResult<TodoItem> findByDueDate (String dueDate, int limit, Map<String, AttributeValue> exclusiveStartKey) {
        QueryRequest.Builder requestBuilder = QueryRequest.builder()
                .tableName( tableName )
                .indexName( DUE_DATE_INDEX )
                .keyConditionExpression( "dueDate = :dueDate" )
                .expressionAttributeValues( Map.of( ":dueDate", AttributeValue.builder().s( dueDate ).build() ) )
                .limit( limit )
                .scanIndexForward( false );
        
        if ( exclusiveStartKey != null && !exclusiveStartKey.isEmpty() ) {
            requestBuilder.exclusiveStartKey( exclusiveStartKey );
        }
        
        return getTodoItemPaginatedResult( exclusiveStartKey, requestBuilder );
    }
    
    /**
     * Retrieves a paginated list of to-do items filtered by status and due date.
     *
     * @param status            The status of the to-do items to filter by.
     * @param dueDate           The due date of the to-do items to filter by.
     * @param limit             The maximum number of items to return.
     * @param exclusiveStartKey The key from the previous query to start from.
     *                          If null, the query starts from the beginning.
     * @return A paginated result containing the filtered to-do items and the last key.
     */
    public PaginatedResult<TodoItem> findByStatusAndDueDate (String status, String dueDate, int limit, Map<String, AttributeValue> exclusiveStartKey) {
        QueryRequest.Builder requestBuilder = QueryRequest.builder()
                .tableName( tableName )
                .indexName( "StatusIndex" )
                .keyConditionExpression( "#status = :status AND dueDate = :dueDate" )
                .expressionAttributeNames( Map.of( "#status", STATUS ) )
                .expressionAttributeValues( Map.of(
                        ":status", AttributeValue.builder().s( status ).build(),
                        ":dueDate", AttributeValue.builder().s( dueDate ).build()
                ) )
                .limit( limit )
                .scanIndexForward( false );
        
        return getTodoItemPaginatedResult( exclusiveStartKey, requestBuilder );
    }
    
    /**
     * Deletes a to-do item by its ID.
     *
     * @param id The ID of the to-do item to delete.
     */
    public void deleteById (String id) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put( ID, AttributeValue.builder().s( id ).build() );
        
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName( tableName )
                .key( key )
                .build();
        
        dynamoDbClient.deleteItem( request );
    }
    
    /**
     * Updates a to-do item.
     *
     * @param id          The ID of the to-do item to update.
     * @param title       The new title of the to-do item.
     * @param description The new description of the to-do item.
     * @param status      The new status of the to-do item.
     * @param dueDate     The new due date of the to-do item.
     */
    public void updateTodo (String id, String title, String description, Status status, String dueDate) {
        Map<String, AttributeValue> key = Map.of(
                ID, AttributeValue.builder().s( id ).build()
        );
        
        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        
        if ( title != null ) {
            updates.put( TITLE, AttributeValueUpdate.builder()
                    .value( AttributeValue.builder().s( title ).build() )
                    .action( AttributeAction.PUT )
                    .build() );
        }
        
        if ( description != null ) {
            updates.put( DESCRIPTION, AttributeValueUpdate.builder()
                    .value( AttributeValue.builder().s( description ).build() )
                    .action( AttributeAction.PUT )
                    .build() );
        }
        
        if ( status != null ) {
            updates.put( STATUS, AttributeValueUpdate.builder()
                    .value( AttributeValue.builder().s( status.name() ).build() )
                    .action( AttributeAction.PUT )
                    .build() );
        }
        
        if ( dueDate != null ) {
            updates.put( DUE_DATE, AttributeValueUpdate.builder()
                    .value( AttributeValue.builder().s( dueDate ).build() )
                    .action( AttributeAction.PUT )
                    .build() );
        }
        
        updates.put( UPDATED_AT, AttributeValueUpdate.builder()
                .value( AttributeValue.builder().s( LocalDateTime.now().toString() ).build() )
                .action( AttributeAction.PUT )
                .build() );
        
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName( tableName )
                .key( key )
                .attributeUpdates( updates )
                .build();
        
        dynamoDbClient.updateItem( request );
    }
    
    /**
     * Updates the status of a to-do item identified by the given ID.
     *
     * @param id        The ID of the to-do item to update.
     * @param newStatus The new status to set for the to-do item.
     */
    public void updateStatus (String id, String newStatus) {
        Map<String, AttributeValue> key = Map.of( "id", AttributeValue.builder().s( id ).build() );
        
        Map<String, AttributeValueUpdate> updates = new HashMap<>();
        updates.put( STATUS, AttributeValueUpdate.builder()
                .value( AttributeValue.builder().s( newStatus ).build() )
                .action( AttributeAction.PUT )
                .build() );
        
        updates.put( UPDATED_AT, AttributeValueUpdate.builder()
                .value( AttributeValue.builder().s( LocalDateTime.now().toString() ).build() )
                .action( AttributeAction.PUT )
                .build() );
        
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName( tableName )
                .key( key )
                .attributeUpdates( updates )
                .build();
        
        dynamoDbClient.updateItem( request );
    }
    
    /**
     * Maps a DynamoDB item to a {@link TodoItem} object.
     *
     * @param item The DynamoDB item to map.
     * @return The mapped {@link TodoItem} object.
     */
    private TodoItem mapToTodoItem (Map<String, AttributeValue> item) {
        return new TodoItem(
                item.get( ID ).s(),
                item.get( TITLE ).s(),
                item.get( DESCRIPTION ).s(),
                item.get( DUE_DATE ).s(),
                item.containsKey( STATUS ) ? item.get( STATUS ).s() : Status.PENDING.name(), // ✅ fallback
                item.get( CREATED_AT ).s(),
                item.containsKey( UPDATED_AT ) ? item.get( UPDATED_AT ).s() : null,            // optional field
                item.get( SORT_KEY ).s()
        );
    }
    
    /**
     * Performs a DynamoDB query and maps the result to a {@link PaginatedResult} of {@link TodoItem}s.
     *
     * @param exclusiveStartKey The last key from the previous query, or null to start from the beginning.
     * @param requestBuilder    The query request builder.
     * @return A paginated result containing the list of {@link TodoItem}s and the last key.
     */
    private PaginatedResult<TodoItem> getTodoItemPaginatedResult (Map<String, AttributeValue> exclusiveStartKey, QueryRequest.Builder requestBuilder) {
        if ( exclusiveStartKey != null && !exclusiveStartKey.isEmpty() )
            requestBuilder.exclusiveStartKey( exclusiveStartKey );
        
        QueryResponse response = dynamoDbClient.query( requestBuilder.build() );
        
        List<TodoItem> todos = response.items().stream()
                .map( this::mapToTodoItem )
                .collect( Collectors.toList() );
        
        // Handle encoding of LastEvaluatedKey
        String lastKeyEncoded = null;
        if ( response.hasLastEvaluatedKey() && !response.lastEvaluatedKey().isEmpty() ) {
            Map<String, AttributeValue> lastEvaluatedKey = response.lastEvaluatedKey();
            String keyString = lastEvaluatedKey.entrySet().stream()
                    .map( entry -> entry.getKey() + "=" + entry.getValue().s() )
                    .collect( Collectors.joining( "," ) );
            lastKeyEncoded = Base64.getEncoder().encodeToString( keyString.getBytes() );
        }
        
        return new PaginatedResult<>( todos, lastKeyEncoded );
    }
}