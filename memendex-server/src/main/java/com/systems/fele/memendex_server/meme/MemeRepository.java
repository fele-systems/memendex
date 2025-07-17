package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.MemendexProperties;
import com.systems.fele.memendex_server.exception.NoSuchMemeError;
import com.systems.fele.memendex_server.model.Meme;
import com.systems.fele.memendex_server.model.MemePayload;
import com.systems.fele.memendex_server.model.MemesType;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
     *
     * @param id ID of the meme
     * @return Optional object containing the Meme, if there`s any
     */
    public Optional<Meme> findById(long id) {
        return jdbcTemplate.query("SELECT * FROM MEMES WHERE id = ? LIMIT 1", MemeRepository::mapRowToMeme, id).stream().findFirst();
    }

    /**
     * Inserts a meme into the repository. This method does not handle
     * file saving, only database operations.
     *
     * @param memePayload The meme to be inserted.
     * @return The newly created meme, with the generated id
     */
    public Meme insert(MemePayload memePayload) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        var createdDate = ZonedDateTime.now();
        jdbcTemplate.update(con -> {
            var stmt = con.prepareStatement("INSERT INTO memes (type_id, filename, description, extension, created_at) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, memePayload.type().getId());
            stmt.setString(2, memePayload.fileName());
            stmt.setString(3, memePayload.description());
            stmt.setString(4, memePayload.extension());
            stmt.setTimestamp(5, Timestamp.from(Instant.from(createdDate)));
            return stmt;
        }, keyHolder);

        var keys = keyHolder.getKeys();
        if (keys == null) throw new RuntimeException("There was an error retrieving the generated key!");

        return new Meme((Long) keys.get("id"),
                memePayload.type(),
                memePayload.fileName(),
                memePayload.description(),
                memePayload.extension(),
                createdDate,
                null);
    }

    private static ZonedDateTime convertToZonedDateTimeUsingLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) return null;
        LocalDateTime localDateTime = timestamp.toLocalDateTime();
        return localDateTime.atZone(ZoneId.systemDefault());
    }

    private static Meme mapRowToMeme(ResultSet rs, int rowNum) throws SQLException {
        return new Meme(
                rs.getLong("id"),
                MemesType.fromId(rs.getInt("type_id")),
                rs.getString("filename"),
                rs.getString("description"),
                rs.getString("extension"),
                convertToZonedDateTimeUsingLocalDateTime(rs.getTimestamp("created_at")),
                convertToZonedDateTimeUsingLocalDateTime(rs.getTimestamp("updated_at")));
    }

    @Deprecated
    public List<Meme> list() {
        return jdbcTemplate.query("SELECT * FROM MEMES", MemeRepository::mapRowToMeme);
    }

    public int getTotalCount() {
        return Objects.requireNonNull(jdbcTemplate.queryForObject("SELECT COUNT(id) FROM MEMES", Integer.class));
    }

    public List<Meme> listPaginated(int pageNum, int pageSize) {
        if (pageNum == 0) pageNum = 1;

        return jdbcTemplate.query("SELECT * FROM MEMES OFFSET ? FETCH FIRST ? ROWS ONLY", MemeRepository::mapRowToMeme, (pageNum - 1) * pageSize, pageSize);
    }

    /**
     * Executes a fuzzy search in memes by using both filename and description fields.
     * <p></p>
     * Notes:
     * pageSize + 1 elements will be queried. This is used to guess if it has reached the
     * end of our query. So when calculating the has next property, check if the size of
     * the returned list is greater than the requested. Also remember to trim the result
     * in this case.
     *
     * @param query    The search term
     * @param pageNum  Current page to fetch
     * @param pageSize Maximum number of elements to return + 1. See notes.
     * @return Memes filtered
     */
    public List<Meme> powerSearch(String query, int pageNum, int pageSize) {
        return jdbcTemplate.query("""
                SELECT * FROM memes
                WHERE TOKEN_SET_PARTIAL_RATIO(description, ?) > 85 OR TOKEN_SET_PARTIAL_RATIO(filename, ?) > 85
                OFFSET ? FETCH FIRST ? ROWS ONLY
                """, MemeRepository::mapRowToMeme, query, query, (pageNum - 1) * pageSize, pageSize + 1);
    }

    /**
     * Updates the fields fileName, description and/or extension, whichever aren't null.
     * @param id id of the meme
     * @param meme update payload
     */
    public void update(long id, MemePayload meme) {
        var params = new MapSqlParameterSource();
        if (meme.description() != null) {
            params.addValue("description", meme.description());
        }

        if (meme.fileName() != null) {
            params.addValue("filename", meme.fileName());
        }

        if (meme.extension() != null) {
            params.addValue("extension", meme.extension());
        }

        // No changes necessary
        if (params.getParameterNames().length == 0) {
            return;
        }

        params.addValue("id", id);

        // Build the SET expression based on which fields where present
        final var sql = "UPDATE memes SET " + Arrays.stream(params.getParameterNames())
                .map(n -> String.format("%s = :%s", n, n))
                .collect(Collectors.joining(", ")) +
                " WHERE id = :id";

        new NamedParameterJdbcTemplate(jdbcTemplate).update(sql, params);
    }

    /**
     * Edits a meme, but only using non-null fields. If fileName is
     * set, then rename file on disk.
     * Any renaming is rolled back if a database error occurs.
     *
     * @param meme The meme update. Only required field is id
     * @deprecated Use {@link #update(long, MemePayload)} instead
     */
    @Deprecated
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

            params.addValue("filename", meme.fileName());
        }

        // Build the SET expression based on which fields where present
        final var sql = "UPDATE memes SET " + Arrays.stream(params.getParameterNames()).map(n -> String.format("%s = :%s", n, n)).collect(Collectors.joining(", ")) + " WHERE id = :id";

        params.addValue("id", meme.id());

        try {
            new NamedParameterJdbcTemplate(jdbcTemplate).update(sql, params);
        } catch (DataAccessException e) {
            // Rollback filename
            if (newFile != null) newFile.renameTo(originalFile);
            throw e;
        }
    }

    /**
     * Updates the "updated" field to current timestamp
     *
     * @param id meme id
     */
    public void touch(long id) {
        jdbcTemplate.update("""
                UPDATE memes
                SET UPDATED = CURRENT_TIMESTAMP(2)
                WHERE id = ?
                """, id);
    }
}
