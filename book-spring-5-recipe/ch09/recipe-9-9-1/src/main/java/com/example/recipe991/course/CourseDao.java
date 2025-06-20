package com.example.recipe991.course;

import java.util.List;

public interface CourseDao {

    Course store(Course course);

    void delete(Long courseId);

    Course findById(Long courseId);

    List<Course> findAll();
}
