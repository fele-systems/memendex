package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.MemendexProperties;
import com.systems.fele.memendex_server.exception.NoSuchMemeError;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class MemeRepository {
    private final JdbcTemplate jdbcTemplate;
    private final MemendexProperties memendexProperties;

    public MemeRepository(JdbcTemplate jdbcTemplate, MemendexProperties memendexProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.memendexProperties = memendexProperties;
    }

    /**
     * Tries to find a meme by ID.
     * @param id ID of the meme
     * @return Optional object containing the Meme, if there`s any
     */
    public Optional<Meme> findById(long id) {
        return jdbcTemplate.query("SELECT * FROM MEMES WHERE id = ? LIMIT 1",
                MemeRepository::mapRowToMeme,
                id).stream().findFirst();
    }

    /**
     * Inserts a meme into the repository. This method does not handle
     * file saving, only database operations.
     *
     * @param meme The meme to be inserted. The @{link {@link Meme#id()}} field is ignored
     * @return The newly created meme, with the generated id
     */
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

    /**
     * Edits a meme, but only using non-null fields. If fileName is
     * set, then rename file on disk.
     * Any renaming is rolled back if a database error occurs.
     *
     * @param meme The meme update. Only required field is id
     */
    public void edit(Meme meme) {
        final var dbMeme = findById(meme.id()).orElseThrow(NoSuchMemeError::new);
        final var uploadLocation = new File(memendexProperties.uploadLocation());
        final var originalFile = new File(uploadLocation, dbMeme.fileName());
        File newFile = null;

        var params = new MapSqlParameterSource();
        if (meme.description() != null) {
            params.addValue("description", meme.description());
        }

        if (meme.fileName() != null) {
            newFile = new File(uploadLocation, meme.fileName());

            if (!originalFile.renameTo(newFile)) {
                throw new RuntimeException("Something went wrong while renaming the file");
            }

            params.addValue("fileName", meme.fileName());
        }

        // Build the SET expression based on which fields where present
        final var sql = "UPDATE MEMES SET " + Arrays.stream(params.getParameterNames())
                .map(n -> String.format("%s = :%s", n, n))
                .collect(Collectors.joining(", ")) + " WHERE id = :id";

        params.addValue("id", meme.id());

        try {
            new NamedParameterJdbcTemplate(jdbcTemplate).update(sql, params);
        } catch (DataAccessException e) {
            // Rollback filename
            if (newFile != null) newFile.renameTo(originalFile);
            throw e;
        }
    }
}
