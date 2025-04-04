package com.systems.fele.memendex_server;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// @Configuration(proxyBeanMethods=false)
@ConfigurationProperties("memendex")
public record MemendexProperties(
        String uploadLocation,
        String cache
) {
}
