package com.systems.fele.memendex_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MemendexProperties.class)
public class MemendexServerApplication {


    public static void main(String[] args) {
		System.setProperty("java.awt.headless", "false");
		SpringApplication.run(MemendexServerApplication.class, args);
	}

}
