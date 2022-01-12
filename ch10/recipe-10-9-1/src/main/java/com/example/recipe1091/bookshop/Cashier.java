package com.example.recipe1091.bookshop;

import java.util.List;


public interface Cashier {
    void checkout(List<String> isbns, String username);
}
