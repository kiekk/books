package com.example.resourceserver.repository;

import com.example.resourceserver.domain.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WorkoutRepository extends JpaRepository<Workout, Integer> {

    @Query("select w from Workout w where w.user = ?#{authentication.name}")
    List<Workout> findAllByUser();
}
