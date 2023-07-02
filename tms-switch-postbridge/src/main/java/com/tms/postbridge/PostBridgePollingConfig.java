package com.tms.postbridge;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class PostBridgePollingConfig {

    @Bean(name = "postbridgePollingScheduler")
    public SchedulerFactoryBean pollingScheduler() {
        SchedulerFactoryBean bean = new SchedulerFactoryBean();
        return bean;
    }
}