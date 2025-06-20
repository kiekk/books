package com.example.studyspringwebflow.entity.support;

import com.example.studyspringwebflow.entity.Book;
import com.example.studyspringwebflow.entity.Category;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BookBuilder extends EntityBuilder<Book> {

	private Book product;

	@Override
	void initProduct() {
		product = new Book();
	}

	public BookBuilder title(String title) {
		product.setTitle(title);
		return this;
	}

	public BookBuilder description(String description) {
		product.setDescription(description);
		return this;
	}

	public BookBuilder author(String author) {
		product.setAuthor(author);
		return this;
	}

	public BookBuilder year(int year) {
		product.setYear(year);
		return this;
	}

	public BookBuilder price(String price) {
		product.setPrice(new BigDecimal(price));
		return this;
	}

	public BookBuilder category(Category category) {
		product.setCategory(category);
		return this;
	}

	public BookBuilder isbn(String isbn) {
		product.setIsbn(isbn);
		return this;
	}

	@Override
	Book assembleProduct() {
		return product;
	}
}