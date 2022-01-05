package com.example.recipe753.board;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
class TodoInitializer {

    private final TodoRepository todoRepository;

    TodoInitializer(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @PostConstruct
    public void setup() {

        Todo todo = new Todo();
        todo.setOwner("marten@ya2do.io");
        todo.setDescription("Finish Spring Recipes - Security Chapter");

        todoRepository.save(todo);

        todo = new Todo();
        todo.setOwner("marten@ya2do.io");
        todo.setDescription("Get Milk & Eggs");
        todo.setCompleted(true);
        todoRepository.save(todo);

        todo = new Todo();
        todo.setOwner("marten@ya2do.io");
        todo.setDescription("Call parents.");

        todoRepository.save(todo);
    }
}
