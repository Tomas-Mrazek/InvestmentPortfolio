package cz.jaktoviditoka.investmentportfolio.model;

import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetGroupedDto;
import cz.jaktoviditoka.investmentportfolio.entity.*;
import cz.jaktoviditoka.investmentportfolio.repository.AssetPriceHistoryRepository;
import cz.jaktoviditoka.investmentportfolio.repository.PortfolioAssetRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
public class Portfolio {

    @Autowired
    PortfolioAssetRepository portfolioAssetRepository;

    @Autowired
    AssetPriceHistoryRepository assetPriceHistoryRepository;

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

    // Seřadit transakce od nejstarších
    // Pro každé datum
    // Vyhledat transakce
    // Pro každou transakci, pokud existuje
    // zjistit, zda-li aktivum na příslušné burze a lokaci a datu existuje
    // pokud ano, přičíst / odečíst sumu
    // pokud ne, vytvořit objekt
    // Pokud neexistuje, zkopírovat objekty s předešlým datem

    public List<PortfolioAssetGroupedDto> calculateValue(Long userId) {
        List<PortfolioAssetGroupedDto> portfolioPerDay = new ArrayList<>();
        List<PortfolioAsset> portfolio = portfolioAssetRepository.findByUserId(userId);
        portfolio.stream()
                .map(el -> el.getDate())
                .min(Comparator.comparing(LocalDate::toEpochDay))
                .orElseThrow()
                .datesUntil(LocalDate.now())
                .forEach(date -> {
                    List<PortfolioAssetGroupedDto> newPortfolioPerDay = new ArrayList<>();
                    portfolioPerDay.stream()
                            .filter(ppd -> Objects.equals(ppd.getDate().plusDays(1), date))
                            .forEach(ppd -> newPortfolioPerDay.add(PortfolioAssetGroupedDto.builder()
                                    .date(ppd.getDate().plusDays(1))
                                    .assetId(ppd.getAssetId())
                                    .amount(ppd.getAmount())
                                    .exchangeId(Objects.nonNull(ppd.getExchangeId()) ? ppd.getExchangeId() : null)
                                    .locationId(Objects.nonNull(ppd.getLocationId()) ? ppd.getLocationId() : null)
                                    .build()));
                    portfolioPerDay.addAll(newPortfolioPerDay);

                    if (portfolio.stream()
                            .filter(pa -> Objects.equals(pa.getDate(), date))
                            .count() > 0) {
                        portfolio.stream()
                                .filter(pa -> Objects.equals(pa.getDate(), date))
                                .forEach(pa -> {
                                    PortfolioAssetGroupedDto pagd = PortfolioAssetGroupedDto.builder()
                                            .date(pa.getDate())
                                            .assetId(pa.getAsset().getId())
                                            .amount(pa.getAmount())
                                            .exchangeId(
                                                    Objects.nonNull(pa.getExchange()) ? pa.getExchange().getId() : null)
                                            .locationId(
                                                    Objects.nonNull(pa.getLocation()) ? pa.getLocation().getId() : null)
                                            .build();

                                    if (portfolioPerDay.stream()
                                            .filter(ppd -> Objects.equals(ppd.getDate(), pagd.getDate()))
                                            .filter(ppd -> Objects.equals(ppd.getAssetId(), pagd.getAssetId()))
                                            .filter(ppd -> Objects.equals(ppd.getExchangeId(), pagd.getExchangeId()))
                                            .filter(ppd -> Objects.equals(ppd.getLocationId(), pagd.getLocationId()))
                                            .count() == 0) {
                                        portfolioPerDay.add(pagd);
                                    } else if (portfolioPerDay.stream()
                                            .filter(ppd -> Objects.equals(ppd.getDate(), pagd.getDate()))
                                            .filter(ppd -> Objects.equals(ppd.getAssetId(), pagd.getAssetId()))
                                            .filter(ppd -> Objects.equals(ppd.getExchangeId(), pagd.getExchangeId()))
                                            .filter(ppd -> Objects.equals(ppd.getLocationId(), pagd.getLocationId()))
                                            .count() == 1) {
                                        portfolioPerDay.stream()
                                                .filter(ppd -> Objects.equals(ppd.getDate(), pagd.getDate()))
                                                .filter(ppd -> Objects.equals(ppd.getAssetId(), pagd.getAssetId()))
                                                .filter(ppd -> Objects.equals(ppd.getExchangeId(),
                                                        pagd.getExchangeId()))
                                                .filter(ppd -> Objects.equals(ppd.getLocationId(),
                                                        pagd.getLocationId()))
                                                .forEach(ppd -> ppd.setAmount(ppd.getAmount().add(pagd.getAmount())));
                                    } else {
                                        throw new IllegalArgumentException("Portfolio error...");
                                    }

                                });

                    }
                });
        return portfolioPerDay;
    }

}
