package com.mediroute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MedirouteApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedirouteApplication.class, args);
	}

}
