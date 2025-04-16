package com.systems.fele.memendex_server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class ApplicationInitializer implements ApplicationListener<ApplicationReadyEvent> {
    private final MemendexProperties memendexProperties;
    private final JdbcTemplate jdbcTemplate;

    public ApplicationInitializer(MemendexProperties memendexProperties, JdbcTemplate jdbcTemplate) {
        this.memendexProperties = memendexProperties;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        createDirectory(memendexProperties.uploadLocation());
        createDirectory(memendexProperties.cache());


    }

    private static void createDirectory(String path) {
        var pathFile = new File(path);
        if (pathFile.exists() && !pathFile.isDirectory())
            throw new RuntimeException("The path %s already exists and it's not a directory".formatted(path));
        else if (!pathFile.exists() && pathFile.mkdirs())
            throw new RuntimeException("Could not create upload directory: %s".formatted(path));
    }
}
