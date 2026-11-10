package ru.skinemsya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SkinemsyaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkinemsyaApplication.class, args);
	}

}
