package com.example.javajigi.dao;

import com.example.javajigi.jdbc.JdbcTemplate;
import com.example.javajigi.jdbc.KeyHolder;
import com.example.javajigi.jdbc.PreparedStatementCreator;
import com.example.javajigi.jdbc.RowMapper;
import com.example.javajigi.model.Answer;
import com.example.javajigi.model.Question;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

public class QuestionDao {

    public Question insert(Question question) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        String sql = "INSERT INTO QUESTIONS (writer, title, contents, createdDate, countOfAnswer) VALUES (?, ?, ?, ?, ?)";
        PreparedStatementCreator psc = con -> {
            PreparedStatement pstmt = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, question.getWriter());
            pstmt.setString(2, question.getTitle());
            pstmt.setString(3, question.getContents());
            pstmt.setTimestamp(4, new Timestamp(question.getTimeFromCreateDate()));
            pstmt.setLong(5, question.getCountOfComment());
            return pstmt;
        };

        KeyHolder keyHolder = new KeyHolder();
        jdbcTemplate.update(psc, keyHolder);
        return findById(keyHolder.getId());
    }
    public List<Question> findAll() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        String sql = "SELECT questionId, writer, title, createdDate, countOfAnswer FROM QUESTIONS "
                + "order by questionId desc";

        RowMapper<Question> rm = rs -> new Question(rs.getLong("questionId"), rs.getString("writer"), rs.getString("title"), null,
                rs.getTimestamp("createdDate"), rs.getInt("countOfAnswer"));

        return jdbcTemplate.query(sql, rm);
    }

    public Question findById(long questionId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
        String sql = "SELECT questionId, writer, title, contents, createdDate, countOfAnswer FROM QUESTIONS "
                + "WHERE questionId = ?";

        RowMapper<Question> rm = rs -> new Question(rs.getLong("questionId"), rs.getString("writer"), rs.getString("title"),
                rs.getString("contents"), rs.getTimestamp("createdDate"), rs.getInt("countOfAnswer"));

        return jdbcTemplate.queryForObject(sql, rm, questionId);
    }
}