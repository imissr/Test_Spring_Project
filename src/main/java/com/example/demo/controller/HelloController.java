package com.example.demo.controller;

import com.example.demo.service.HelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class HelloController {

    @Autowired
    private HelloService service;

    @RequestMapping("/")
    @ResponseBody
    public String sayHello() {
        return service.getMessage();
    }
}
