package cz.jaktoviditoka.investmentportfolio.service;

import cz.jaktoviditoka.investmentportfolio.entity.*;
import cz.jaktoviditoka.investmentportfolio.repository.LedgerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LedgerService {

    @Autowired
    LedgerRepository ledgerRepository;

    public List<Ledger> getEntries(AppUser appUser) {
        return ledgerRepository.findByUser(appUser);
    }
    
    public Ledger createEntry(BigDecimal amount, Asset asset, Location location, Exchange exchange, Transaction transaction) {
        Ledger ledger = Ledger.builder()
                .date(transaction.getTimestamp().toLocalDate())
                .user(transaction.getUser())
                .amount(amount)
                .asset(asset)
                .exchange(exchange)
                .location(location)
                .transaction(transaction)
                .build();
        return ledgerRepository.save(ledger);
    }

}
