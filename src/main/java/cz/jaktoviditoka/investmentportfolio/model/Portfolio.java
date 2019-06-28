package cz.jaktoviditoka.investmentportfolio.model;

import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetGroupedDto;
import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetPerDayValueDto;
import cz.jaktoviditoka.investmentportfolio.entity.*;
import cz.jaktoviditoka.investmentportfolio.repository.AssetPriceHistoryRepository;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.PortfolioAssetRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class Portfolio {

    @Autowired
    AssetRepository assetRepository;

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

    public List<PortfolioAssetGroupedDto> portfolioPerDay(Long userId) {
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

    public List<PortfolioAssetPerDayValueDto> portfolioPerDayValue(Long userId) {
        Asset defaultCurrency = assetRepository.findByTicker("CZK").orElseThrow();
        List<PortfolioAssetPerDayValueDto> portfolioAssetPerDayValue = new ArrayList<>();

        List<AssetPriceHistory> assetPriceHistory = assetPriceHistoryRepository.findAll();
        List<PortfolioAssetGroupedDto> portfolioPerDay = portfolioPerDay(userId);
        portfolioPerDay.stream()
                .map(el -> el.getDate())
                .distinct()
                .forEach(date -> {
                    List<BigDecimal> values = new ArrayList<>();
                    List<PortfolioAssetGroupedDto> assets = new ArrayList<>();

                    portfolioPerDay.stream()
                            .filter(ppd -> Objects.equals(ppd.getDate(), date))
                            .forEach(ppd -> {

                                if (Objects.equals(defaultCurrency, assetRepository.findById(ppd.getAssetId()).get())) {
                                    values.add(ppd.getAmount());
                                    assets.add(ppd);
                                } else if (assetPriceHistory.stream()
                                        .filter(aph -> Objects.equals(aph.getAsset().getId(), ppd.getAssetId()))
                                        .filter(aph -> Objects.equals(aph.getExchange().getId(), ppd.getExchangeId()))
                                        .count() > 0) {

                                    BigDecimal closingPrice = assetPriceHistory.stream()
                                            .filter(aph -> Objects.equals(aph.getDate(), ppd.getDate()))
                                            .filter(aph -> Objects.equals(aph.getAsset().getId(), ppd.getAssetId()))
                                            .filter(aph -> Objects.equals(aph.getExchange().getId(),
                                                    ppd.getExchangeId()))
                                            .map(aph -> aph.getClosingPrice())
                                            .findFirst()
                                            .orElseGet(() -> assetPriceHistory.stream()
                                                    .filter(aph -> aph.getDate().isBefore(ppd.getDate()))
                                                    .filter(aph -> Objects.equals(aph.getAsset().getId(),
                                                            ppd.getAssetId()))
                                                    .filter(aph -> Objects.equals(aph.getExchange().getId(),
                                                            ppd.getExchangeId()))
                                                    .max((el1, el2) -> el1.getDate().compareTo(el2.getDate()))
                                                    .orElseThrow()
                                                    .getClosingPrice());

                                    values.add(ppd.getAmount().multiply(closingPrice));
                                    assets.add(ppd);

                                }

                            });

                    BigDecimal value = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal lastValue = portfolioAssetPerDayValue.stream()
                            .max((el1, el2) -> el1.getDate().compareTo(el2.getDate()))
                            .orElseGet(() -> PortfolioAssetPerDayValueDto.builder().value(BigDecimal.ZERO).build())
                            .getValue();

                    PortfolioAssetPerDayValueDto papdv = PortfolioAssetPerDayValueDto.builder()
                            .date(date)
                            .value(value)
                            .change(value.subtract(lastValue))
                            .percentualChange(lastValue.compareTo(BigDecimal.ZERO) != 0
                                    ? value.subtract(lastValue).divide(lastValue, 4, RoundingMode.HALF_UP)
                                            .multiply(BigDecimal.valueOf(100))
                                    : null)
                            .assets(assets)
                            .build();

                    portfolioAssetPerDayValue.add(papdv);
                });

        return portfolioAssetPerDayValue;
    }

}
