package com.example.demo.repository;

import org.springframework.stereotype.Repository;

@Repository
public class HelloRepository {
    public String fetchGreeting() {
        return "Hello from Structurizr!";
    }
}
