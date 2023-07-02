package com.tms.service.config;

import com.tms.lib.router.Router;
import com.tms.lib.router.RuleRouter;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@ComponentScan("com.tms")
@EntityScan("com.tms")
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.tms")
@EnableAsync
public class AppConfig {

    @Bean
    public Router router(){
        return new RuleRouter();
    }
}
