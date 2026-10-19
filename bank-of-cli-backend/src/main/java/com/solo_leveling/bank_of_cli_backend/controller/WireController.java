package com.solo_leveling.bank_of_cli_backend.controller;

import com.solo_leveling.bank_of_cli_backend.entity.Account;
import com.solo_leveling.bank_of_cli_backend.service.WireService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wire")
public class WireController {

    WireService wireService;

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
        //Possibly talk to service layer for
        List<Account> accounts = List.of(
                new Account(1,"checking",100,1)
        )


        return ResponseEntity.ok(wireService.executeWireTransfer());
    }

}
