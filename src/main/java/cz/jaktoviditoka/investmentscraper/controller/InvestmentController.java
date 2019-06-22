package cz.jaktoviditoka.investmentscraper.controller;

import cz.jaktoviditoka.investmentscraper.dto.*;
import cz.jaktoviditoka.investmentscraper.entity.*;
import cz.jaktoviditoka.investmentscraper.job.AssetPriceHistoryJob;
import cz.jaktoviditoka.investmentscraper.model.FioEbrokerScraper;
import cz.jaktoviditoka.investmentscraper.repository.*;
import cz.jaktoviditoka.investmentscraper.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class InvestmentController {

    private static String PASSWORD = "TestPW45";

    @Autowired
    UserRepository userRepository;

    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    PortfolioAssetRepository portfolioAssetRepository;
    
    @Autowired
    AssetPriceHistoryRepository assetPriceHistoryRepository;

    @Autowired
    TransactionService transactionService;
    
    @Autowired
    AssetPriceHistoryJob assetPriceHistory;

    @Autowired
    FioEbrokerScraper fioEbrokerScraper;
    
    @Autowired
    ModelMapper modelMapper;

    @GetMapping("/users")
    public List<User> getUsers(@RequestHeader("password") String password) {
        if (password.equals(PASSWORD)) {
            return userRepository.findAll();
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @GetMapping("/users/{id}")
    public User getUserById(@RequestHeader("password") String password, @RequestParam Long id) {
        if (password.equals(PASSWORD)) {
            Optional<User> userOpt = userRepository.findById(id);
            if (userOpt.isPresent()) {
                return userOpt.get();
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/users")
    public void createUser(@RequestBody User user) {
        userRepository.save(user);
    }

    @GetMapping("/exchanges")
    public List<Exchange> getExchanges() {
        return exchangeRepository.findAll();
    }

    @GetMapping("/exchanges/{id}")
    public Exchange getExchangesById(@RequestParam Long id) {
        Optional<Exchange> exchangeOpt = exchangeRepository.findById(id);
        if (exchangeOpt.isPresent()) {
            return exchangeOpt.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/exchanges")
    public void createExchange(@RequestBody Exchange exchange) {
        exchangeRepository.save(exchange);
    }

    @GetMapping("/assets")
    public List<Asset> getAssets() {
        return assetRepository.findAll();
    }

    @GetMapping("/assets/{id}")
    public Asset getAssetById(@RequestParam Long id) {
        Optional<Asset> assetOpt = assetRepository.findById(id);
        if (assetOpt.isPresent()) {
            return assetOpt.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/assets")
    public void createAsset(@RequestBody Asset asset) {
        assetRepository.save(asset);
    }

    @GetMapping("/locations")
    public List<Location> getLocations() {
        return locationRepository.findAll();
    }

    @GetMapping("/locations/{id}")
    public Location getLocationById(@RequestParam Long id) {
        Optional<Location> locationOpt = locationRepository.findById(id);
        if (locationOpt.isPresent()) {
            return locationOpt.get();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/locations")
    public void createLocation(@RequestBody Location location) {
        locationRepository.save(location);
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> getTransactionByUser(@RequestParam Long userId) {
        List<TransactionResponse> transactions = transactionRepository.findByUserId(userId).stream()
                .map(map -> modelMapper.map(map, TransactionResponse.class))
                .collect(Collectors.toList());
        return transactions;
    }

    @PostMapping("/transactions/deposit")
    public void deposit(@RequestBody TransactionDepositRequest request) {
        Transaction transaction = modelMapper.map(request, Transaction.class);
        modelMapper.validate();
        transactionService.process(transaction);
    }

    @PostMapping("/transactions/withdrawal")
    public void withdrawal(@RequestBody TransactionWithdrawalRequest request) {
        Transaction transaction = modelMapper.map(request, Transaction.class);
        modelMapper.validate();
        transactionService.process(transaction);
    }

    @PostMapping("/transactions/transfer")
    public void transfer(@RequestBody TransactionTransferRequest request) {
        Transaction transaction = modelMapper.map(request, Transaction.class);
        modelMapper.validate();
        transactionService.process(transaction);
    }

    @PostMapping("/transactions/trade")
    public void trade(@RequestBody TransactionTradeRequest request) {
        log.debug("TransactionDto: {}", request);

        TransactionPart buy = modelMapper.map(request.getBuy(), TransactionPart.class);
        TransactionPart sell = modelMapper.map(request.getSell(), TransactionPart.class);
        log.debug("TransactionPart buy: {}", buy);
        log.debug("TransactionPart sell: {}", sell);

        Transaction transaction = modelMapper.map(request, Transaction.class);
        log.debug("Transaction: {}", transaction);

        // modelMapper.validate();
        transactionService.process(transaction);
    }

    @PostMapping("/transactions/interest")
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
    
    @GetMapping("/fioEbrokerTransactions")
    public List<Transaction> getFioEbrokerTransactions() throws IOException {
        return fioEbrokerScraper.getTransactions();
    }

}
