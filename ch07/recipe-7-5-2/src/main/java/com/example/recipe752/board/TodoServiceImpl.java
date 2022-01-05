package com.example.recipe752.board;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('USER')")
    public List<Todo> listTodos() {
        return todoRepository.findAll();
    }

    @Override
    @PreAuthorize("hasAuthority('USER')")
    public void save(Todo todo) {
        this.todoRepository.save(todo);
    }

    @Override
    @PreAuthorize("hasAuthority('USER')")
    public void complete(long id) {
        Todo todo = findById(id);
        todo.setCompleted(true);
        todoRepository.save(todo);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public void remove(long id) {
        todoRepository.remove(id);
    }

    @Override
    @PreAuthorize("hasAuthority('USER')")   // 메서드 호출 직전 실행
    @PostAuthorize("returnObject.owner == authentication.name") // 메서드 호출 직후 실행
    public Todo findById(long id) {
        return todoRepository.findOne(id);
    }
}

