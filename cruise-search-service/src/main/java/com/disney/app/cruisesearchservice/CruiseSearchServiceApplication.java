package com.disney.app.cruisesearchservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CruiseSearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CruiseSearchServiceApplication.class, args);
    }
}
