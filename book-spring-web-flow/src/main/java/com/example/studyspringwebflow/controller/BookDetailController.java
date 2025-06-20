package com.example.studyspringwebflow.controller;

import com.example.studyspringwebflow.entity.Book;
import com.example.studyspringwebflow.service.BookstoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller to handle book detail requests.
 * 
 * @author Marten Deinum
 * @author Koen Serneels
 * 
 */
@Controller
@RequiredArgsConstructor
public class BookDetailController {

	private final BookstoreService bookstoreService;

	/**
	 * Method used to prepare our model and select the view to show the details of the selected book.
	 * @param bookId the id of the book
	 * @param model the implicit model
	 * @return view name to render (book/detail)
	 */
	@RequestMapping(value = "/book/detail/{bookId}")
	public String details(@PathVariable("bookId")
	long bookId, Model model) {
		Book book = this.bookstoreService.findBook(bookId);
		model.addAttribute(book);
		return "book/detail";
	}
}
