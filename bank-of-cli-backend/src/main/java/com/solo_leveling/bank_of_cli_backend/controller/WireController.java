package com.solo_leveling.bank_of_cli_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Controller
@RestController
@RequestMapping("/wire")
public class WireController {

    @GetMapping
    ResponseEntity<String> wireTest() {

        //This controller will take two users and one amount
        //It will authorize
        //It will add the amount to target while removing it from source

        return ResponseEntity.ok("Successful! GOOD JOB YDUR, YOU GOT THIS ");
    }

    @PostMapping
    ResponseEntity<String> wireTransfer(String src, String dest, Integer amount) {

        //This controller will take two users and one amount
        //It will authorize
        //It will add the amount to target while removing it from source
        //Possibly talk to service layer for logic


        return ResponseEntity.ok("Successful! GOOD JOB YDUR, YOU GOT THIS ");
    }

}
