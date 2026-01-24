package ru.galtor85.household_store.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public String home() {
        return "Household Store API is running!<br>" +
                "Health check: <a href='/actuator/health'>/actuator/health</a><br>" +
                "Info: <a href='/actuator/info'>/actuator/info</a>";
    }

    @GetMapping("/api/health")
    public String health() {
        return "Custom health endpoint: OK";
    }
}