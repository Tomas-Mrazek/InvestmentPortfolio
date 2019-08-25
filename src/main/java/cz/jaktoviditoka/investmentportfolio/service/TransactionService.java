package cz.jaktoviditoka.investmentportfolio.service;

import cz.jaktoviditoka.investmentportfolio.entity.Transaction;
import cz.jaktoviditoka.investmentportfolio.entity.TransactionPart;
import cz.jaktoviditoka.investmentportfolio.model.Portfolio;
import cz.jaktoviditoka.investmentportfolio.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
public class TransactionService {

    @Autowired
    TransactionRepository transactionRepository;
    
    @Autowired
    Portfolio portfolio;

    @Transactional
    public void process(Transaction transaction) {
        log.debug("Transaction: {}", transaction);
        transactionRepository.save(transaction);
        
        if (Objects.nonNull(transaction.getOut())) {
            TransactionPart part = transaction.getOut();
            portfolio.remove(transaction, part.getAsset(), part.getAmount(), part.getLocation(), part.getExchange());
            if (Objects.nonNull(part.getFeeAmount())) {
                portfolio.remove(transaction, part.getFeeAsset(), part.getFeeAmount(), part.getLocation(), part.getExchange());
            }
        }
        
        if (Objects.nonNull(transaction.getIn())) {
            TransactionPart part = transaction.getIn();
            portfolio.add(transaction, part.getAsset(), part.getAmount(), part.getLocation(), part.getExchange());
            if (Objects.nonNull(part.getFeeAmount())) {
                portfolio.remove(transaction, part.getFeeAsset(), part.getFeeAmount(), part.getLocation(), part.getExchange());
            }
        }

    }    
    
}
