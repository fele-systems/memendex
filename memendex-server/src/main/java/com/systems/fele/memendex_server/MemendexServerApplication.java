package com.systems.fele.memendex_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MemendexProperties.class)
public class MemendexServerApplication {


    public static void main(String[] args) {
		SpringApplication.run(MemendexServerApplication.class, args);
	}

}
