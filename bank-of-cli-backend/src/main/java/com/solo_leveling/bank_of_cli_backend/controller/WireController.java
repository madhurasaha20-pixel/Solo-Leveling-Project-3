package com.solo_leveling.bank_of_cli_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Controller
@RestController
@RequestMapping("/wire")
public class WireController {

    @GetMapping
    ResponseEntity<String> wire() {
        return ResponseEntity.ok("Successful!");
    }

}
