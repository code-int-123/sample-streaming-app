package com.checkout.steaming_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class PageViewEventsPublisherApplication {

	public static void main(String[] args) {
		SpringApplication.run(PageViewEventsPublisherApplication.class, args);
	}

}