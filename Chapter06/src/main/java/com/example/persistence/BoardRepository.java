package com.example.persistence;

import org.springframework.data.repository.CrudRepository;

import com.example.domain.Board;

public interface BoardRepository extends CrudRepository<Board, Long> {

}
