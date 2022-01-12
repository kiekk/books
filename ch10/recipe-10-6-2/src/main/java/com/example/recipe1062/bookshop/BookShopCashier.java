package com.example.recipe1062.bookshop;

import java.util.List;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


public class BookShopCashier implements Cashier {
    private BookShop bookShop;

    public void setBookShop(BookShop bookShop) {
        this.bookShop = bookShop;
    }

    /*
        Propagation.REQUIRES_NEW : 무조건 트랜잭션을 새로 생성, 진행 중인 트랜잭션이 있으면 중단
        Propagation.SUPPORTS : 트랜잭션이 있으면 메서드를 해당 트랜잭션 내에서 실행, 없을 경우 트랜잭션 없이 메서드 실행
        Propagation.NOT_SUPPORTED : 트랜잭션 없이 메서드를 실행, 진행 중인 트랜잭션이 있을 경우 중단
        Propagation.MANDATORY : 반드시 트랜잭션을 걸고 메서드를 실행, 진행 중인 트랜잭션이 없으면 예외를 던짐
        Propagation.NEVER : 반드시 트랜잭션 없이 메서드를 실행, 진행 중인 트랜잭션이 있으면 예외를 던짐
        Propagation.NESTED : 진행 중인 트랜잭션이 있으면 메서드를 해당 트랜잭션의 중첩 트랜잭션 내에서 실행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkout(List<String> isbns, String username) {
        for (String isbn : isbns) {
            bookShop.purchase(isbn, username);
        }
    }
}
