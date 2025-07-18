package com.systems.fele.memendex_server;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

@Component
public class ApplicationInitializer implements ApplicationListener<ApplicationReadyEvent> {
    private final MemendexProperties memendexProperties;
    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public ApplicationInitializer(MemendexProperties memendexProperties, JdbcTemplate jdbcTemplate, Environment environment) {
        this.memendexProperties = memendexProperties;
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        createDirectory(memendexProperties.uploadLocation());
        createDirectory(memendexProperties.cache());

        try {
            var metadata = Objects.requireNonNull(this.jdbcTemplate.getDataSource()).getConnection().getMetaData();
            var rs = metadata.getTables(null, "PUBLIC", null, new String[] { "TABLE" });
            if (!rs.next()) {
                initializeDatabase();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private void initializeDatabase() {
        executeScriptFromClassPath(jdbcTemplate, "schema.sql");
        if (environment.matchesProfiles("development")) {
            executeScriptFromClassPath(jdbcTemplate, "data.sql");
        }
    }

    private static void executeScriptFromClassPath(JdbcTemplate jdbcTemplate, String scriptFileName) {
        jdbcTemplate.execute(new ConnectionCallback<Void>() {
            @Override
            @Nullable
            public Void doInConnection(@NonNull Connection con) throws DataAccessException {
                var resource = new ClassPathResource(scriptFileName);
                ScriptUtils.executeSqlScript(con, resource);
                return null;
            }
        });
    }

    private static void createDirectory(String path) {
        var pathFile = new File(path);
        if (pathFile.exists() && !pathFile.isDirectory())
            throw new RuntimeException("The path %s already exists and it's not a directory".formatted(path));
        else if (!pathFile.exists() && pathFile.mkdirs())
            throw new RuntimeException("Could not create upload directory: %s".formatted(path));
    }
}
