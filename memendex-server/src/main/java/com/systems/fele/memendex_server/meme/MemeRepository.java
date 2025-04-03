package com.systems.fele.memendex_server.meme;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class MemeRepository {
    private final JdbcTemplate jdbcTemplate;

    public MemeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Meme insert(Meme meme) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var stmt = con.prepareStatement("INSERT INTO MEMES (FILENAME, DESCRIPTION) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, meme.fileName());
            stmt.setString(2, meme.description());
            return stmt;
        }, keyHolder);

        var keys = keyHolder.getKeys();
        if (keys == null)
            throw new RuntimeException("There was an error retrieving the generated key!");

        return new Meme((Long) keys.get("id"), meme.fileName(), meme.description());
    }

    private static Meme mapRowToMeme(ResultSet rs, int rowNum) throws SQLException {
        return new Meme(rs.getLong("id"), rs.getString("filename"), rs.getString("description"));
    }

    // TODO: Implement pagination
    public List<Meme> list() {
        return jdbcTemplate.query("SELECT * FROM MEMES",
                MemeRepository::mapRowToMeme);
    }

    public List<Meme> powerSearch(String query) {
        return jdbcTemplate.query("SELECT * FROM MEMES",
                MemeRepository::mapRowToMeme).stream()
                .filter(meme -> meme.fileName().toLowerCase().contains(query) || meme.description().toLowerCase().contains(query))
                .toList();
    }
}
