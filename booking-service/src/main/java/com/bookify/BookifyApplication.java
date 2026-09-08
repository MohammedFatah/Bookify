package com.bookify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class BookifyApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(("Asia/Kokata")));
		SpringApplication.run(BookifyApplication.class, args);
	}

}
