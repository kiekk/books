package com.example.studyspringwebflow.repository;

import com.example.studyspringwebflow.entity.Category;

import java.util.List;

public interface CategoryRepository {

	List<Category> findAll();

	Category findById(long id);

	void storeCategory(Category category);
}
