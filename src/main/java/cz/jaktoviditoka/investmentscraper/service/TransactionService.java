package cz.jaktoviditoka.investmentscraper.service;

import cz.jaktoviditoka.investmentscraper.entity.Transaction;
import cz.jaktoviditoka.investmentscraper.entity.TransactionPart;
import cz.jaktoviditoka.investmentscraper.model.Portfolio;
import cz.jaktoviditoka.investmentscraper.model.KurzyCzScraper;
import cz.jaktoviditoka.investmentscraper.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Transactional
@Service
public class TransactionService {

    @Autowired
    TransactionRepository transactionRepository;
    
    @Autowired
    Portfolio portfolio;
    
    @Autowired
    KurzyCzScraper scraper;

    public void process(Transaction transaction) {
        log.debug("Transaction: {}", transaction);
        transactionRepository.save(transaction);
        
        if (Objects.nonNull(transaction.getFrom())) {
            TransactionPart part = transaction.getFrom();
            portfolio.remove(transaction, part.getAsset(), part.getAmount(), part.getLocation(), part.getExchange());
            if (Objects.nonNull(part.getFeeAmount())) {
                portfolio.remove(transaction, part.getFeeAsset(), part.getFeeAmount(), part.getLocation(), part.getExchange());
            }
        }
        
        if (Objects.nonNull(transaction.getTo())) {
            TransactionPart part = transaction.getTo();
            portfolio.add(transaction, part.getAsset(), part.getAmount(), part.getLocation(), part.getExchange());
            if (Objects.nonNull(part.getFeeAmount())) {
                portfolio.remove(transaction, part.getFeeAsset(), part.getFeeAmount(), part.getLocation(), part.getExchange());
            }
        }

    }
    
    
    
}
