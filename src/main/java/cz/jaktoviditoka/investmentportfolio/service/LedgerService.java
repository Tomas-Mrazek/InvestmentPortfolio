package cz.jaktoviditoka.investmentportfolio.service;

import cz.jaktoviditoka.investmentportfolio.dto.LedgerResponse;
import cz.jaktoviditoka.investmentportfolio.entity.*;
import cz.jaktoviditoka.investmentportfolio.repository.LedgerRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@Service
public class LedgerService {

    @Autowired
    LedgerRepository ledgerRepository;

    @Autowired
    ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<LedgerResponse> getEntries(AppUser appUser) {
        return ledgerRepository.findByUser(appUser)
                .stream()
                .map(map -> modelMapper.map(map, LedgerResponse.class))
                .collect(Collectors.toList());
    }
    
    @Transactional
    public Ledger createEntry(Transaction transaction, Exchange exchange, String location, Asset asset,
            BigDecimal amount) {
        Ledger ledger = Ledger.builder()
                .timestamp(transaction.getTimestamp())
                .user(transaction.getUser())
                .transaction(transaction)
                .exchange(exchange)
                .location(location)
                .asset(asset)
                .amount(amount)
                .build();
        return ledgerRepository.save(ledger);
    }

}
