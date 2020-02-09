package cz.jaktoviditoka.investmentportfolio.service;

import cz.jaktoviditoka.investmentportfolio.entity.Transaction;
import cz.jaktoviditoka.investmentportfolio.entity.TransactionMovement;
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
    LedgerService ledgerService;

    @Transactional
    public void process(Transaction transaction) {
        log.debug("{}", transaction);
        transactionRepository.save(transaction);
        
        if (Objects.nonNull(transaction.getOut())) {
            TransactionMovement movement = transaction.getOut();
            ledgerService.createEntry(movement.getAmount().negate(), movement.getAsset(), movement.getLocation(), movement.getExchange(), transaction);
            if (Objects.nonNull(movement.getFeeAmount())) {
                ledgerService.createEntry(movement.getFeeAmount().negate(), movement.getFeeAsset(), movement.getLocation(), movement.getExchange(), transaction);
            }
        }
        
        if (Objects.nonNull(transaction.getIn())) {
            TransactionMovement movement = transaction.getIn();
            ledgerService.createEntry(movement.getAmount(), movement.getAsset(), movement.getLocation(), movement.getExchange(), transaction);
            if (Objects.nonNull(movement.getFeeAmount())) {
                ledgerService.createEntry(movement.getFeeAmount().negate(), movement.getFeeAsset(), movement.getLocation(), movement.getExchange(), transaction);
            }
        }

    }    
    
}
