package com.example.ratelimit;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hello")
public class MyController {

    @GetMapping("/{name}")
    public String hello(@PathVariable(name = "name") String name) {
        return "Hello, " + name + "!";
    }

}
