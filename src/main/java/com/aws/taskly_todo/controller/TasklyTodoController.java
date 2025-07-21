package com.aws.taskly_todo.controller;

import com.aws.taskly_todo.service.TasklyTodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TasklyTodoController {
    private final TasklyTodoService tasklyTodoService;
}
