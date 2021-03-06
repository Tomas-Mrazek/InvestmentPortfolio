package cz.tomastokamrazek.arion.service;

import cz.tomastokamrazek.arion.entity.Transaction;
import cz.tomastokamrazek.arion.entity.TransactionMovement;
import cz.tomastokamrazek.arion.repository.TransactionRepository;
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
        transactionRepository.save(transaction);

        if (Objects.nonNull(transaction.getOut())) {
            TransactionMovement movement = transaction.getOut();
            ledgerService.createEntry(transaction,
                    movement.getExchange(),
                    movement.getLocation(),
                    movement.getAsset(),
                    movement.getAmount().negate());
            if (Objects.nonNull(movement.getFeeAmount())) {
                ledgerService.createEntry(transaction,
                        movement.getExchange(),
                        movement.getLocation(),
                        movement.getFeeAsset(),
                        movement.getFeeAmount().negate());
            }
        }

        if (Objects.nonNull(transaction.getIn())) {
            TransactionMovement movement = transaction.getIn();
            ledgerService.createEntry(transaction,
                    movement.getExchange(),
                    movement.getLocation(),
                    movement.getAsset(),
                    movement.getAmount());
            if (Objects.nonNull(movement.getFeeAmount())) {
                ledgerService.createEntry(transaction,
                        movement.getExchange(),
                        movement.getLocation(),
                        movement.getFeeAsset(),
                        movement.getFeeAmount().negate());
            }
        }

    }

}
