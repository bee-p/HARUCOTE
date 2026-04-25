package org.project.cote;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CoteApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoteApplication.class, args);
    }

}
