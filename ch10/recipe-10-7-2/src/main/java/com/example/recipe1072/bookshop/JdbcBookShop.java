package com.example.recipe1072.bookshop;

import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


public class JdbcBookShop extends JdbcDaoSupport implements BookShop {

    @Transactional
    public void purchase(String isbn, String username) {
        int price = getJdbcTemplate().queryForObject(
                "SELECT PRICE FROM BOOK WHERE ISBN = ?", Integer.class, isbn);

        getJdbcTemplate().update(
                "UPDATE BOOK_STOCK SET STOCK = STOCK - 1 WHERE ISBN = ?", isbn );

        getJdbcTemplate().update(
                "UPDATE ACCOUNT SET BALANCE = BALANCE - ? WHERE USERNAME = ?", price, username);
    }

    @Transactional
    public void increaseStock(String isbn, int stock) {
        String threadName = Thread.currentThread().getName();
        System.out.println(threadName + " - Prepare to increase book stock");

        getJdbcTemplate().update("UPDATE BOOK_STOCK SET STOCK = STOCK + ? WHERE ISBN = ?", stock, isbn);

        System.out.println(threadName + " - Book stock increased by " + stock);
        sleep(threadName);

        System.out.println(threadName + " - Book stock rolled back");
        throw new RuntimeException("Increased by mistake");
    }

    /*
        Isolation.READ_UNCOMMITTED : 한 트랜잭션이 다른 트랜잭션이 아직 커밋하기전에 변경한 내용을 읽을 수 있음
        Isolation.READ_COMMITTED : 한 트랜잭션이 다른 트랜잭션이 아직 커밋하기전에 변경한 내용을 읽을 수 없음
        Isolation.REPEATABLE_READ : 트랜잭션이 어떤 필드를 여러 번 읽어도 동일한 값을 읽도록 보장, 트랜잭션이 지속되는 동안 다른 트랜잭션이 해당 필드를 변경할 수 없음
        Isolation.SERIALIZABLE : 트랜잭션이 테이블을 여러 번 읽어도 동일한 row 를 읽도록 보장, 트랜잭션이 지속되는 동안 다른 트랜잭션이 해당 테이블에 삽입, 수정, 삭제를 할 수 없음
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int checkStock(String isbn) {
        String threadName = Thread.currentThread().getName();
        System.out.println(threadName + " - Prepare to check book stock");

        int stock = getJdbcTemplate().queryForObject("SELECT STOCK FROM BOOK_STOCK WHERE ISBN = ?", Integer.class, isbn);

        System.out.println(threadName + " - Book stock is " + stock);
        sleep(threadName);

        return stock;
    }

    private void sleep(String threadName) {
        System.out.println(threadName + " - Sleeping");

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
        }

        System.out.println(threadName + " - Wake up");
    }
}
