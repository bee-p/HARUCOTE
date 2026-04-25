package org.project.cote.common.security.jwt;

import org.project.cote.common.security.oauth2.OAuth2Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, OAuth2Properties.class})
public class JwtConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
