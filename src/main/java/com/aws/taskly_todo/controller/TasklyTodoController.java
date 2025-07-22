package com.aws.taskly_todo.controller;

import com.aws.taskly_todo.model.PaginatedResult;
import com.aws.taskly_todo.model.Status;
import com.aws.taskly_todo.model.TodoItem;
import com.aws.taskly_todo.service.TasklyTodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping( "/todos" )
public class TasklyTodoController {
    private final TasklyTodoService tasklyTodoService;
    
    @GetMapping
    public String listTodos (
            @RequestParam( required = false ) String status,
            @RequestParam( required = false ) String dueDate,
            @RequestParam( required = false ) String lastKey,
            @RequestParam( defaultValue = "10" ) int limit,
            Model model
    ) {
        PaginatedResult<TodoItem> result = tasklyTodoService.getTodos( status, dueDate, limit, lastKey );
        
        model.addAttribute( "todos", result.items() );
        model.addAttribute( "statuses", Status.values() );
        model.addAttribute( "selectedStatus", status );
        model.addAttribute( "selectedDueDate", dueDate );
        model.addAttribute( "lastKey", result.lastEvaluatedKey() );
        return "todos-list";
    }
    
    @GetMapping( "/{id}" )
    public String viewTodo (@PathVariable String id, Model model) {
        TodoItem todo = tasklyTodoService.getTodoItem( id );
        model.addAttribute( "todo", todo );
        return "todo-view";
    }
    
    @GetMapping( "/new" )
    public String showCreateForm (Model model) {
        model.addAttribute( "todo", new TodoItem() );
        model.addAttribute( "statuses", Status.values() );
        return "todo-form";
    }
    
    @PostMapping
    public String createTodo (@ModelAttribute TodoItem todo) {
        tasklyTodoService.createTodo( todo.getTitle(), todo.getDescription(), todo.getDueDate() );
        return "redirect:/todos";
    }
    
    @GetMapping( "/{id}/edit" )
    public String showEditForm (@PathVariable String id, Model model) {
        TodoItem todo = tasklyTodoService.getTodoItem( id );
        
        model.addAttribute( "todo", todo );
        model.addAttribute( "statuses", Status.values() );
        model.addAttribute("isEdit", true);
        return "todo-form";
    }
    
    @PutMapping( "/{id}" )
    public String updateTodo (@PathVariable String id, @ModelAttribute TodoItem todo) {
        tasklyTodoService.updateTodo( id, todo.getTitle(),
                todo.getDescription(), todo.getDueDate(),
                Status.valueOf( todo.getStatus() ) );
        return "redirect:/todos";
    }
    
    @PostMapping( "/{id}/delete" )
    public String deleteTodo (@PathVariable String id) {
        tasklyTodoService.deleteTodo( id );
        return "redirect:/todos";
    }
}
