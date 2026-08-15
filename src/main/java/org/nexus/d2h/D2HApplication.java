package org.nexus.d2h;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class D2HApplication {

    public static void main(String[] args) {
        SpringApplication.run(D2HApplication.class, args);
    }

}
