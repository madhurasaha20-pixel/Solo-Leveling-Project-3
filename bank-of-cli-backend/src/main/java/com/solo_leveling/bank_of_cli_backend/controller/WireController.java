package com.solo_leveling.bank_of_cli_backend.controller;
import com.solo_leveling.bank_of_cli_backend.service.WireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;


@RestController
@RequestMapping("/wire")
public class WireController {

    private final WireService wireService;

    @Autowired
    public WireController(WireService wireService) {
        this.wireService = wireService;
    }

    @GetMapping
    ResponseEntity<String> wireTest() {

        return ResponseEntity.ok("Successful! GOOD JOB YDUR, YOU GOT THIS ");
    }
    //This method will accept three(3) parameters as represented by the path & path-variables
    @PostMapping("/{fromAccountId}/{toAccountId}/{amount}")
    public ResponseEntity wireTransfer(@PathVariable String fromAccountId, @PathVariable String toAccountId, @PathVariable BigDecimal amount) {
        //It will authorize
        //It call the service layer method to execute the transaction
        wireService.executeWireTransfer(fromAccountId,toAccountId,amount);
        return ResponseEntity.noContent().build();
    }
}
