package cz.jaktoviditoka.investmentportfolio.service;

import cz.jaktoviditoka.investmentportfolio.entity.Transaction;
import cz.jaktoviditoka.investmentportfolio.entity.TransactionPart;
import cz.jaktoviditoka.investmentportfolio.model.Portfolio;
import cz.jaktoviditoka.investmentportfolio.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Transactional
@Service
public class TransactionService {

    @Autowired
    TransactionRepository transactionRepository;
    
    @Autowired
    Portfolio portfolio;

    public void process(Transaction transaction) {
        log.debug("Transaction: {}", transaction);
        transactionRepository.save(transaction);
        
        if (Objects.nonNull(transaction.getRemove())) {
            TransactionPart part = transaction.getRemove();
            portfolio.remove(transaction, part.getAsset(), part.getAmount(), part.getLocation(), part.getExchange());
            if (Objects.nonNull(part.getFeeAmount())) {
                portfolio.remove(transaction, part.getFeeAsset(), part.getFeeAmount(), part.getLocation(), part.getExchange());
            }
        }
        
        if (Objects.nonNull(transaction.getAdd())) {
            TransactionPart part = transaction.getAdd();
            portfolio.add(transaction, part.getAsset(), part.getAmount(), part.getLocation(), part.getExchange());
            if (Objects.nonNull(part.getFeeAmount())) {
                portfolio.remove(transaction, part.getFeeAsset(), part.getFeeAmount(), part.getLocation(), part.getExchange());
            }
        }

    }
    
    public void process(List<Transaction> transactions) {
        transactions.forEach(el -> process(el));
    }
    
    
    
}
