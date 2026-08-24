package com.amigoscode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Array;
import java.util.Arrays;

@SpringBootApplication
@RestController
public class Application {

    public static void main(String[] args) {

        SpringApplication.run(Application.class, args);
    }

    @GetMapping
    public int  getMessage() {
//        int[] numbers = {1, 2, 3, 4, 5};
        int[] numbers = {1, 2, 3};

        return numbers[2];
    }

}
