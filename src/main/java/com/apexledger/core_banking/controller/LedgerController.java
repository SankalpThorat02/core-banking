package com.apexledger.core_banking.controller;

import com.apexledger.core_banking.dto.DepositRequest;
import com.apexledger.core_banking.dto.TransferRequest;
import com.apexledger.core_banking.dto.TransferResponse;
import com.apexledger.core_banking.entity.Transaction;
import com.apexledger.core_banking.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerService ledgerService;
    public LedgerController(LedgerService ledgerService){
        this.ledgerService = ledgerService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest requestDto) {
        Transaction completedTxn = ledgerService.transferFunds(
                requestDto.getFromAccount(),
                requestDto.getToAccount(),
                requestDto.getAmount(),
                requestDto.getIdempotencyKey()
        );

        TransferResponse response = new TransferResponse(
                completedTxn.getUuid().toString(),
                completedTxn.getStatus(),
                completedTxn.getAmount(),
                completedTxn.getTimestamp()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransferResponse> deposit(@Valid @RequestBody DepositRequest requestDto) {
        Transaction completedTxn = ledgerService.depositFunds(
                requestDto.getToAccount(),
                requestDto.getAmount(),
                requestDto.getIdempotencyKey()
        );

        TransferResponse response = new TransferResponse(
                completedTxn.getUuid().toString(),
                completedTxn.getStatus(),
                completedTxn.getAmount(),
                completedTxn.getTimestamp()
        );

        return ResponseEntity.ok(response);
    }
}
