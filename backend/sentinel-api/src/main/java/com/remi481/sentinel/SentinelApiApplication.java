package com.remi481.sentinel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SentinelApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(SentinelApiApplication.class, args);
	}

}
