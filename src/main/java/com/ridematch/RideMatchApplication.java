package com.ridematch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.ridematch.config.MatchingProperties;
import com.ridematch.config.CancellationPolicyProperties;

/** Starts the RideMatch Spring Boot application and enables entity auditing. */
@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties({MatchingProperties.class, CancellationPolicyProperties.class})
public class RideMatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(RideMatchApplication.class, args);
    }
}
