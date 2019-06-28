package cz.jaktoviditoka.investmentportfolio.controller;

import cz.jaktoviditoka.investmentportfolio.dto.*;
import cz.jaktoviditoka.investmentportfolio.dto.transaction.*;
import cz.jaktoviditoka.investmentportfolio.entity.AppUser;
import cz.jaktoviditoka.investmentportfolio.entity.Transaction;
import cz.jaktoviditoka.investmentportfolio.entity.TransactionPart;
import cz.jaktoviditoka.investmentportfolio.model.FioEbrokerScraper;
import cz.jaktoviditoka.investmentportfolio.repository.AppUserRepository;
import cz.jaktoviditoka.investmentportfolio.repository.TransactionRepository;
import cz.jaktoviditoka.investmentportfolio.security.HasAnyAuthority;
import cz.jaktoviditoka.investmentportfolio.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
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
        TransactionPart buy = modelMapper.map(request.getBuy(), TransactionPart.class);
        TransactionPart sell = modelMapper.map(request.getSell(), TransactionPart.class);
        Transaction transaction = modelMapper.map(request, Transaction.class);
        transaction.setRemove(sell);
        transaction.setAdd(buy);
        transactionService.process(transaction);
    }

    @PostMapping("/interest")
    public void interest(@RequestBody TransactionInterestRequest request) {
        Transaction transaction = modelMapper.map(request, Transaction.class);
        modelMapper.validate();
        transactionService.process(transaction);
    }

    @GetMapping("/import/fioEbroker")
    public void getFioEbrokerTransactions(@RequestBody(required = false) AppUserFioEbrokerRequest request)
            throws IOException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserRepository.findByEmail(email).orElseThrow();
        if (Objects.isNull(request)) {
            if (Objects.isNull(user.getFioEbrokerUsername()) || Objects.isNull(user.getFioEbrokerPassword())) {
                throw new IllegalArgumentException("Fio e-Broker credentials not provided.");
            }
            transactionService.process(fioEbrokerScraper.getTransactions(user));
        } else {
            transactionService.process(fioEbrokerScraper.getTransactions(request.getUsername(), request.getPassword(), user));
        }

    }

}
