package com.example.recipe10101.bookshop;

import java.util.List;


public interface Cashier {
    void checkout(List<String> isbns, String username);
}
