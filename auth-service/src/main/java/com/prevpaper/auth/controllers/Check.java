package com.prevpaper.auth.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/check")
public class Check {
    @GetMapping
    String check(){
        System.out.println("Reuest for /check");
        return "correct";
    }


}
