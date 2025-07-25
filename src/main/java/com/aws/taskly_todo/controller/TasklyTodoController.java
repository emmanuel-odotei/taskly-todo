package com.aws.taskly_todo.controller;

import com.aws.taskly_todo.model.PaginatedResult;
import com.aws.taskly_todo.model.Status;
import com.aws.taskly_todo.model.TodoItem;
import com.aws.taskly_todo.service.TasklyTodoService;
import com.aws.taskly_todo.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping( "/" )
public class TasklyTodoController {
    private final TasklyTodoService tasklyTodoService;
    
    /**
     * List To-do items with pagination.
     *
     * @param status The status filter to apply. Optional.
     * @param dueDate The due date filter to apply. Optional.
     * @param lastKey The last key from the previous query. Optional.
     * @param tokenStack The token stack from the previous query. Optional.
     * @param limit The page size. Defaults to 10.
     * @param model The model to populate with attributes.
     * @return The view name, which is "todos-list".
     */
    @GetMapping
    public String listTodos (
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dueDate,
            @RequestParam(required = false) String lastKey,
            @RequestParam(required = false) String tokenStack,
            @RequestParam(defaultValue = "10") int limit,
            Model model
    ) {
        // Get paginated result from service FIRST
        PaginatedResult<TodoItem> result = tasklyTodoService.getTodos(status, dueDate, limit, lastKey);
        
        // Build the CURRENT page's token stack using the PREVIOUS request's lastKey
        String currentTokenStack = updateTokenStack(tokenStack, lastKey);
        
        // Extract previous page token using the current token stack
        Map<String, String> prevPage = PaginationUtils.getPreviousPageToken(currentTokenStack);
        
        // Send attributes to view
        model.addAttribute("todos", result.items());
        model.addAttribute("statuses", Status.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedDueDate", dueDate);
        model.addAttribute("lastKey", result.lastEvaluatedKey());
        model.addAttribute("tokenStack", currentTokenStack);
        model.addAttribute("prevLastKey", prevPage.get("lastKey"));
        model.addAttribute("prevTokenStack", prevPage.get("tokenStack"));
        model.addAttribute("limit", limit);
        
        return "todos-list";
    }
    
    /**
     * Shows a single to-do item.
     * @param id The ID of the to-do item to show.
     * @param model The model to populate with attributes.
     * @return The view name, which is "to-do-view".
     */
    @GetMapping( "todos/{id}" )
    public String viewTodo (@PathVariable String id, Model model) {
        TodoItem todo = tasklyTodoService.getTodoItem( id );
        model.addAttribute( "todo", todo );
        return "todo-view";
    }
    
    /**
     * Shows the create form.
     * @param model The model to populate with attributes.
     * @return The view name, which is "to-do-form".
     */
    @GetMapping( "todos/new" )
    public String showCreateForm (Model model) {
        model.addAttribute( "todo", new TodoItem() );
        model.addAttribute( "statuses", Status.values() );
        return "todo-form";
    }
    
    /**
     * Creates a new to-do item and redirects to the to-do list.
     *
     * @param todoItem The to-do item to be created, populated from the form submission.
     * @return A redirect to the list of todos.
     */
    @PostMapping( "todos" )
    public String createTodo (@ModelAttribute TodoItem todoItem) {
        tasklyTodoService.createTodo( todoItem.getTitle(), todoItem.getDescription(), todoItem.getDueDate() );
        return "redirect:/";
    }
    
    /**
     * Shows the edit form.
     * @param id The ID of the to-do item to show.
     * @param model The model to populate with attributes.
     * @return The view name, which is "to-do-form".
     */
    @GetMapping( "todos/{id}/edit" )
    public String showEditForm (@PathVariable String id, Model model) {
        TodoItem todo = tasklyTodoService.getTodoItem( id );
        
        model.addAttribute( "todo", todo );
        model.addAttribute( "statuses", Status.values() );
        model.addAttribute("isEdit", true);
        return "todo-form";
    }
    
    /**
     * Updates a to-do item.
     * @param id The ID of the to-do item to be updated.
     * @param todoItem The updated to-do item, populated from the form submission.
     * @return A redirect to the list of todos.
     */
    @PutMapping( "todos/{id}" )
    public String updateTodo (@PathVariable String id, @ModelAttribute TodoItem todoItem) {
        tasklyTodoService.updateTodo( id, todoItem.getTitle(),
                todoItem.getDescription(), todoItem.getDueDate(),
                Status.valueOf( todoItem.getStatus() ) );
        return "redirect:/";
    }
    
    /**
     * Deletes a to-do item by its ID and redirects to the to-do list.
     *
     * @param id The ID of the to-do item to be deleted.
     * @return A redirect to the list of todos.
     */
    @PostMapping( "todos/{id}/delete" )
    public String deleteTodo (@PathVariable String id) {
        tasklyTodoService.deleteTodo( id );
        return "redirect:/";
    }
    
    /**
     * Updates the status of a to-do item.
     * @param id The ID of the to-do item to update.
     * @param status The new status of the to-do item.
     * @return A redirect to the list of todos.
     */
    @PostMapping("/todos/update-status")
    public String updateStatus(@RequestParam String id, @RequestParam Status status) {
        tasklyTodoService.updateStatus(id, status.name());
        return "redirect:/";
    }
    
    //helper methods
    /**
     * Updates the token stack by appending the last key.
     *
     * @param tokenStack The current stack of tokens as a comma-separated string.
     *                   Can be null or blank if no tokens exist.
     * @param lastKey The last key to append to the token stack.
     *                If null or empty, the token stack remains unchanged.
     * @return The updated token stack with the last key appended,
     *         or the original token stack if the last key is null or empty.
     */
    public String updateTokenStack(String tokenStack, String lastKey) {
        if (lastKey == null || lastKey.isEmpty()) return tokenStack;
        
        return (tokenStack == null || tokenStack.isBlank())
                ? lastKey
                : tokenStack + "," + lastKey;
    }
}