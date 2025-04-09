package com.bookstore.trigger;

import org.h2.api.Trigger;

import java.sql.Connection;
import java.sql.SQLException;

public class ReviewTrigger implements Trigger {

    @Override
    public void init(Connection conn, String schemaName,
                     String triggerName, String tableName, boolean before, int type) {
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        // 인덱스는 review 테이블 컬럼 순서 기준
        Long articleId = (Long) newRow[0];   // article_id
        Long bookId = (Long) newRow[1];   // book_id
        Long magazineId = (Long) newRow[2];      // magazine_id

        if ((articleId != null && bookId != null)
                || (articleId != null && magazineId != null)
                || (bookId != null && magazineId != null)) {
            throw new SQLException("A review can be associated with either a book, a magazine or an article");
        }

    }

    @Override
    public void close() {
    }

    @Override
    public void remove() {
    }
}
