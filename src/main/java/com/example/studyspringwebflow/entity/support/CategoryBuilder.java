package com.example.studyspringwebflow.entity.support;

import com.example.studyspringwebflow.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryBuilder extends EntityBuilder<Category> {

	@Override
	void initProduct() {
	}

	@Override
	Category assembleProduct() {
		return this.product;
	}

	public CategoryBuilder name(String name) {
		this.product = new Category(name);
		return this;
	}

}
