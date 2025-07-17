package com.systems.fele.memendex_server.meme;

import com.systems.fele.memendex_server.model.Tag;
import com.systems.fele.memendex_server.model.TagToMeme;
import com.systems.fele.memendex_server.model.TagUsage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class TagToMemeRepository {
    private final JdbcTemplate jdbcTemplate;

    public TagToMemeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns the tag id of the tag(s) related to a meme
     *
     * @param memeId id of the meme
     * @return array of tag ids
     */
    public long[] getTagsRelatedToMeme(long memeId) {
        return jdbcTemplate.query("SELECT TAG_ID FROM TAGS_TO_MEMES WHERE MEME_ID = ?", TagToMemeRepository::resultSetToId, memeId)
                .stream()
                .mapToLong(Long::longValue)
                .toArray();
    }

    /**
     * Returns the relation object between tags and a meme
     *
     * @param memeId id of the meme
     * @return list of relation objects
     */
    public List<TagToMeme> getRelationsToMeme(long memeId) {
        return jdbcTemplate.query("SELECT * FROM TAGS_TO_MEMES WHERE MEME_ID = ?", TagToMemeRepository::resultSetToTagToMeme, memeId);
    }

    public TagToMeme createRelation(long tagId, long memeId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var stmt = con.prepareStatement("INSERT INTO TAGS_TO_MEMES (TAG_ID, MEME_ID) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            stmt.setLong(1, tagId);
            stmt.setLong(2, memeId);
            return stmt;
        }, keyHolder);

        var keys = keyHolder.getKeys();
        if (keys == null)
            throw new RuntimeException("There was an error retrieving the generated key!");

        return new TagToMeme((Long) keys.get("id"), tagId, memeId);
    }

    public void deleteRelation(long relationId) {
        jdbcTemplate.update("DELETE FROM TAGS_TO_MEMES WHERE ID = ?", relationId);
    }

    public int countTagReferences(long tagId) {
        var count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TAGS_TO_MEMES WHERE TAG_ID = ?", Integer.class, tagId);
        return count == null ? 0 : count;
    }

    private static long resultSetToId(ResultSet resultSet, int i) throws SQLException {
        return resultSet.getLong(1);
    }

    private static TagToMeme resultSetToTagToMeme(ResultSet resultSet, int i) throws SQLException {
        return new TagToMeme(resultSet.getLong("ID"), resultSet.getLong("TAG_ID"), resultSet.getLong("MEME_ID"));
    }

    public record TagIdUsage(long tagId, long count) {

    }

    public List<TagUsage> getTopTags() {
        return jdbcTemplate.query("""
                        SELECT scope, name, COUNT(*) AS COUNT
                        FROM tags_to_memes
                        INNER JOIN tags ON tags_to_memes.tag_id = tags.id
                        GROUP BY tag_id ORDER BY COUNT DESC FETCH FIRST 10 ROWS ONLY""",
                TagToMemeRepository::tagUsageFromResultSet
        );
    }

    public List<TagUsage> getSuggestions(String searchTerm) {
        return jdbcTemplate.query("""
                        SELECT scope, name, COUNT(*) AS COUNT
                        FROM tags_to_memes
                        INNER JOIN TAGS ON tags_to_memes.tag_id = TAGS.ID
                        WHERE CONCAT(scope, '/', name) LIKE CONCAT('%', ?, '%')
                        GROUP BY tag_id
                        ORDER BY COUNT DESC
                        FETCH FIRST 10 ROWS ONLY
                        """,
                TagToMemeRepository::tagUsageFromResultSet,
                searchTerm
        );
    }

    private static TagUsage tagUsageFromResultSet(ResultSet resultSet, int i) throws SQLException {
        return new TagUsage(new Tag(0, resultSet.getString("scope"), resultSet.getString("name")).toString(), resultSet.getInt("count"));
    }
}
