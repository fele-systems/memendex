package com.systems.fele.memendex_server.tag;

import com.systems.fele.memendex_server.model.Tag;
import com.systems.fele.memendex_server.MemendexProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class TagRepository {
    private final JdbcTemplate jdbcTemplate;
    private final MemendexProperties memendexProperties;

    public TagRepository(JdbcTemplate jdbcTemplate, MemendexProperties memendexProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.memendexProperties = memendexProperties;
    }

    /**
     * Adds a tag into the repository
     *
     * @param scope Optional. The scope of the tag i.e. the value before '/'
     * @param value The value or value of the tag.
     * @return The newly created tag
     */
    public Tag addOrFindTag(String scope, String value) {
        var myScope = Objects.requireNonNull(scope).toLowerCase();
        var myValue = value == null ? null : value.toLowerCase();

        var existingTag = getTag(myScope, myValue);
        if (existingTag.isPresent()) return existingTag.get();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var stmt = con.prepareStatement("INSERT INTO TAGS (SCOPE, \"VALUE\") VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, myScope);
            stmt.setString(2, myValue);
            return stmt;
        }, keyHolder);

        var keys = keyHolder.getKeys();
        if (keys == null)
            throw new RuntimeException("There was an error retrieving the generated key!");

        return new Tag((long) keys.get("id"), myScope, myValue);
    }

    public Optional<Tag> getTag(long id) {
        return jdbcTemplate.query("SELECT * FROM TAGS WHERE id = ? LIMIT 1",
                TagRepository::mapRowToTag,
                id).stream().findFirst();
    }

    public Optional<Tag> getTag(String scope, String value) {
        return value == null ?
                jdbcTemplate.query("SELECT * FROM TAGS WHERE SCOPE = ? AND \"VALUE\" IS NULL LIMIT 1", TagRepository::mapRowToTag,
                        scope).stream().findFirst()
                :
                jdbcTemplate.query("SELECT * FROM TAGS WHERE SCOPE = ? AND \"VALUE\" = ? LIMIT 1", TagRepository::mapRowToTag,
                        scope, value).stream().findFirst();
    }

    public List<Tag> search(String searchTerm) {
        return jdbcTemplate.query("SELECT * FROM TAGS WHERE CONCAT(SCOPE, '/', \"VALUE\") LIKE CONCAT('%', ?, '%')",
                TagRepository::mapRowToTag,
                searchTerm);
    }

    private static Tag mapRowToTag(ResultSet rs, int rowNum) throws SQLException {
        return new Tag(rs.getLong("id"), rs.getString("SCOPE"), rs.getString("VALUE"));
    }

    public void deleteTag(long tagId) {
        jdbcTemplate.update("DELETE FROM TAGS WHERE ID = ?", tagId);
    }
}
