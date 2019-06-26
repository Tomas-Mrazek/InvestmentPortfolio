package cz.jaktoviditoka.investmentportfolio.controller;

import cz.jaktoviditoka.investmentportfolio.dto.*;
import cz.jaktoviditoka.investmentportfolio.entity.AssetPriceHistory;
import cz.jaktoviditoka.investmentportfolio.entity.Transaction;
import cz.jaktoviditoka.investmentportfolio.entity.TransactionPart;
import cz.jaktoviditoka.investmentportfolio.job.AssetPriceHistoryJob;
import cz.jaktoviditoka.investmentportfolio.model.FioEbrokerScraper;
import cz.jaktoviditoka.investmentportfolio.repository.AssetPriceHistoryRepository;
import cz.jaktoviditoka.investmentportfolio.repository.PortfolioAssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.TransactionRepository;
import cz.jaktoviditoka.investmentportfolio.security.HasAnyAuthority;
import cz.jaktoviditoka.investmentportfolio.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@HasAnyAuthority
@RestController
@RequestMapping("/transactions")
public class TransactionController {
    
    @Autowired
    TransactionRepository transactionRepository;
    
    @Autowired
    AssetPriceHistoryRepository assetPriceHistoryRepository;
    
    @Autowired
    PortfolioAssetRepository portfolioAssetRepository;
    
    @Autowired
    TransactionService transactionService;
    
    @Autowired
    AssetPriceHistoryJob assetPriceHistory;
    
    @Autowired
    ModelMapper modelMapper;
    
    @Autowired
    FioEbrokerScraper fioEbrokerScraper;

    @GetMapping
    public List<TransactionResponse> getTransactionByUser(@RequestParam Long userId) {
        return transactionRepository.findByUserId(userId).stream()
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
        log.debug("TransactionDto: {}", request);

        TransactionPart buy = modelMapper.map(request.getBuy(), TransactionPart.class);
        TransactionPart sell = modelMapper.map(request.getSell(), TransactionPart.class);
        log.debug("TransactionPart buy: {}", buy);
        log.debug("TransactionPart sell: {}", sell);

        Transaction transaction = modelMapper.map(request, Transaction.class);
        log.debug("Transaction: {}", transaction);

        transactionService.process(transaction);
    }

    @PostMapping("/interest")
    public void interest(@RequestBody TransactionInterestRequest request) {
        Transaction transaction = modelMapper.map(request, Transaction.class);
        modelMapper.validate();
        transactionService.process(transaction);
    }

    @GetMapping("/portfolioAssets")
    public List<PortfolioAssetResponse> getPortfolioAssets(@RequestParam Long userId) {
        return portfolioAssetRepository.findByUserId(userId).stream()
                .map(el -> modelMapper.map(el, PortfolioAssetResponse.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/assetPriceHistory")
    public List<AssetPriceHistory> getAssetPriceHistory() throws IOException, InterruptedException {
        assetPriceHistory.createMissingRecords();
        return assetPriceHistoryRepository.findAll();
    }

    @GetMapping("/importTransactions")
    public List<Transaction> getFioEbrokerTransactions(@RequestParam("from") String from) throws IOException {
        return fioEbrokerScraper.getTransactions();
    }
    
}
