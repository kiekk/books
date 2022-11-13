package com.example.studyspringwebflow.service;

import com.example.studyspringwebflow.entity.Category;

import java.util.List;

/**
 * Contract for services that work with an {@link Category}.
 *
 * @author Marten Deinum
 * @author Koen Serneels
 */
public interface CategoryService {

    Category findById(long id);

    List<Category> findAll();

    void addCategory(Category category);

}
