package com.systems.fele.memendex_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MemendexProperties.class)
public class MemendexServerApplication {


    public static void main(String[] args) {
		var headless = System.getenv().getOrDefault("MEMENDEX_HEADLESS", "");

		if (headless.isEmpty()) {
			System.setProperty("java.awt.headless", "false");
		} else {
			System.out.println("Picked up environment variable MEMENDEX_HEADLESS with value " + headless + ". Running in headless mode");
		}

		SpringApplication.run(MemendexServerApplication.class, args);
	}

}
