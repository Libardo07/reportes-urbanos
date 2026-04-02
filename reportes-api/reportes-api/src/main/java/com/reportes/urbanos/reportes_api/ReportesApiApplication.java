package com.reportes.urbanos.reportes_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ReportesApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReportesApiApplication.class, args);



	}

}
