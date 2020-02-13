package cz.jaktoviditoka.arion.controller;

import cz.jaktoviditoka.arion.dto.AppUserFioEbrokerRequest;
import cz.jaktoviditoka.arion.dto.transaction.*;
import cz.jaktoviditoka.arion.entity.AppUser;
import cz.jaktoviditoka.arion.entity.Transaction;
import cz.jaktoviditoka.arion.entity.TransactionMovement;
import cz.jaktoviditoka.arion.model.FioEbrokerScraper;
import cz.jaktoviditoka.arion.repository.AppUserRepository;
import cz.jaktoviditoka.arion.repository.TransactionRepository;
import cz.jaktoviditoka.arion.security.HasAnyAuthority;
import cz.jaktoviditoka.arion.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@HasAnyAuthority
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    TransactionService transactionService;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    FioEbrokerScraper fioEbrokerScraper;

    @GetMapping
    public List<TransactionResponse> getTransactions() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return transactionRepository.findByUserEmail(username).stream()
                .map(map -> modelMapper.map(map, TransactionResponse.class))
                .collect(Collectors.toList());
    }

    @PostMapping("/deposit")
    public void deposit(@RequestBody TransactionDepositRequest request) {
        Transaction transaction = modelMapper.map(request, Transaction.class);
        modelMapper.validate();
        transactionService.process(transaction);
    }

    @PostMapping("/withdrawal")
    public void withdrawal(@RequestBody TransactionWithdrawalRequest request) {
        Transaction transaction = modelMapper.map(request, Transaction.class);
        modelMapper.validate();
        transactionService.process(transaction);
    }

    @PostMapping("/transfer")
    public void transfer(@RequestBody TransactionTransferRequest request) {
        Transaction transaction = modelMapper.map(request, Transaction.class);
        modelMapper.validate();
        transactionService.process(transaction);
    }

    @PostMapping("/trade")
    public void trade(@RequestBody TransactionTradeRequest request) {
        TransactionMovement buy = modelMapper.map(request.getBuy(), TransactionMovement.class);
        TransactionMovement sell = modelMapper.map(request.getSell(), TransactionMovement.class);
        Transaction transaction = modelMapper.map(request, Transaction.class);
        transaction.setOut(sell);
        transaction.setIn(buy);
        transactionService.process(transaction);
    }

    @PostMapping("/interest")
    public void interest(@RequestBody TransactionInterestRequest request) {
        Transaction transaction = modelMapper.map(request, Transaction.class);
        modelMapper.validate();
        transactionService.process(transaction);
    }

    @GetMapping("/import/fioEbroker")
    public void getFioEbrokerTransactions(
            @RequestBody Optional<AppUserFioEbrokerRequest> request)
            throws IOException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserRepository.findByUsername(email).orElseThrow();
        if (request.isEmpty()) {
            if (Objects.isNull(user.getFioEbrokerUsername()) || Objects.isNull(user.getFioEbrokerPassword())) {
                throw new IllegalArgumentException("Fio e-Broker credentials not provided.");
            }
            fioEbrokerScraper.getTransactions(user)
                    .forEach(el -> transactionService.process(el));
        } else {
            fioEbrokerScraper.getTransactions(request.get().getUsername(), request.get().getPassword(), user)
                    .forEach(el -> transactionService.process(el));
        }

    }

}
