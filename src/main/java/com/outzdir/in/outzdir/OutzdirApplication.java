package com.outzdir.in.outzdir;

import javax.sound.sampled.Port;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OutzdirApplication {

	public static void main(String[] args) {
		SpringApplication.run(OutzdirApplication.class, args);
		System.out.println("Outzidr Application Started");
		System.out.println("Server ruuning on port 8080");
	}

}
