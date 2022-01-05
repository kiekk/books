package com.example.recipe751.board;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;

    TodoServiceImpl(TodoRepository todoRepository) {
        this.todoRepository = todoRepository;
    }

    @Override
    @Secured("USER")
    public List<Todo> listTodos() {
        return todoRepository.findAll();
    }

    @Override
    @Secured("USER")
    public void save(Todo todo) {
        this.todoRepository.save(todo);
    }

    @Override
    @Secured("USER")
    public void complete(long id) {
        Todo todo = findById(id);
        todo.setCompleted(true);
        todoRepository.save(todo);
    }

    @Override
    @Secured({"USER", "ADMIN"})
    public void remove(long id) {
        todoRepository.remove(id);
    }

    @Override
    @Secured("USER")
    public Todo findById(long id) {
        return todoRepository.findOne(id);
    }
}

