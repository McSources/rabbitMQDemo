package com.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@Slf4j
public class SpringbootRabbitmqApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext run = SpringApplication.run(SpringbootRabbitmqApplication.class, args);
//        String[] beanDefinitionNames = run.getBeanDefinitionNames();
//        List<String> list = Arrays.asList(beanDefinitionNames);
    }

}
