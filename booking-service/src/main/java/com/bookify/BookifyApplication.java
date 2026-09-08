package com.bookify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;
import java.time.ZoneId;

@SpringBootApplication
public class BookifyApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Kolkata")));
		SpringApplication.run(BookifyApplication.class, args);
	}

}
