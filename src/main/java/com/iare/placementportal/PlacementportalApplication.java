package com.iare.placementportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
		"org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration"
})
public class PlacementportalApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlacementportalApplication.class, args);
	}

}
