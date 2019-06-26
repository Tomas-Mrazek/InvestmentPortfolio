package cz.jaktoviditoka.investmentportfolio.model;

import cz.jaktoviditoka.investmentportfolio.domain.PortfolioAssetGrouped;
import cz.jaktoviditoka.investmentportfolio.entity.*;
import cz.jaktoviditoka.investmentportfolio.repository.PortfolioAssetRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class Portfolio {

    @Autowired
    PortfolioAssetRepository portfolioAssetRepository;
    
    @Autowired
    ModelMapper modelMapper;

    public void add(Transaction transaction, Asset asset, BigDecimal amount, Location location, Exchange exchange) {
        PortfolioAsset portfolioAsset = PortfolioAsset.builder()
                .date(transaction.getTimestamp().toLocalDate())
                .user(transaction.getUser())
                .asset(asset)
                .amount(amount)
                .location(location)
                .exchange(exchange)
                .transaction(transaction)
                .build();
        portfolioAssetRepository.save(portfolioAsset);
    }

    public void remove(Transaction transaction, Asset asset, BigDecimal amount, Location location, Exchange exchange) {
        PortfolioAsset portfolioAsset = PortfolioAsset.builder()
                .date(transaction.getTimestamp().toLocalDate())
                .user(transaction.getUser())
                .asset(asset)
                .amount(amount.negate())
                .location(location)
                .exchange(exchange)
                .transaction(transaction)
                .build();
        portfolioAssetRepository.save(portfolioAsset);
    }

    public void calculateValue(Long userId) {
        List<PortfolioAsset> portfolioAssets = portfolioAssetRepository.findByUserId(userId);

        List<LocalDate> portfolioChangeDates = portfolioAssets.stream()
                .map(el -> el.getDate())
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        LocalDate date = null;

        List<PortfolioAssetGrouped> portfolioMovements = portfolioAssets.stream()
                .filter(el -> el.getDate().isBefore(date))
                .map(el -> modelMapper.map(el, PortfolioAssetGrouped.class))
                .collect(Collectors.groupingBy(
                        PortfolioAssetGrouped::getAsset,
                        Collectors.groupingBy(
                                PortfolioAssetGrouped::getExchange,
                                Collectors.reducing(
                                        BigDecimal.ZERO,
                                        PortfolioAssetGrouped::getAmount,
                                        BigDecimal::add))))
                .entrySet()
                .stream()
                .flatMap(e1 -> e1.getValue()
                     .entrySet()
                     .stream()
                     .map(e2 -> new PortfolioAssetGrouped(e1.getKey(), e2.getValue(), e2.getKey())))
                .collect(Collectors.toList());

    }

}
