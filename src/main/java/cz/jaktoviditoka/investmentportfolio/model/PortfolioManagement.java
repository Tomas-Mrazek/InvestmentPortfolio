package cz.jaktoviditoka.investmentportfolio.model;

import cz.jaktoviditoka.investmentportfolio.domain.ExchangeAbbrEnum;
import cz.jaktoviditoka.investmentportfolio.domain.PortfolioAsset;
import cz.jaktoviditoka.investmentportfolio.domain.PortfolioAssetPerDay;
import cz.jaktoviditoka.investmentportfolio.domain.TransactionType;
import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetResponse;
import cz.jaktoviditoka.investmentportfolio.entity.*;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.ExchangeRepository;
import cz.jaktoviditoka.investmentportfolio.repository.LedgerRepository;
import cz.jaktoviditoka.investmentportfolio.repository.PriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PortfolioManagement {

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    LedgerRepository ledgerRepository;

    @Autowired
    PriceRepository assetPriceRepository;

    @Autowired
    ModelMapper modelMapper;

    private static final String PRICE_ASSET = "CZK";

    public BigDecimal amountInvested(AppUser appUser) {
        List<Ledger> portfolio = ledgerRepository.findByUser(appUser);
        return portfolio.stream()
                .filter(el -> Objects.equals(el.getTransaction().getType(), TransactionType.DEPOSIT))
                .map(mapper -> mapper.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal value(AppUser appUser) {
        return portfolioPerDayTest(appUser)
                .stream()
                .map(mapper -> mapper.getValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<PortfolioAssetResponse> portfolioPerDayTest(AppUser appUser) {
        Asset priceAsset = assetRepository.findByName(PRICE_ASSET)
                .orElseThrow(() -> new IllegalArgumentException("Price asset not found."));
        List<Ledger> portfolio = ledgerRepository.findByUser(appUser);
        return portfolio.stream()
                .collect(
                        Collectors.groupingBy(Ledger::getAsset,
                                Collectors.groupingBy(Ledger::getExchange,
                                        Collectors.groupingBy(Ledger::getLocation,
                                                Collectors.reducing(BigDecimal.ZERO, Ledger::getAmount,
                                                        BigDecimal::add)))))
                .entrySet()
                .stream()
                .flatMap(asset -> asset.getValue().entrySet()
                        .stream()
                        .flatMap(exchange -> exchange.getValue().entrySet()
                                .stream()
                                .map(location -> {
                                    Optional<BigDecimal> price = findPrice(asset.getKey(), exchange.getKey(),
                                            priceAsset);

                                    BigDecimal amount = location.getValue();

                                    return PortfolioAssetResponse.builder()
                                            .assetId(asset.getKey().getId())
                                            .assetName(asset.getKey().getName())
                                            .assetTicker(asset.getKey().getTicker())
                                            .assetType(asset.getKey().getType())
                                            .exchange(exchange.getKey().getAbbreviation().name())
                                            .location(location.getKey().getName())
                                            .amount(amount)
                                            .value(price.isPresent()
                                                    ? amount.multiply(price.get()).setScale(18, RoundingMode.HALF_UP)
                                                    : BigDecimal.ZERO)
                                            .build();
                                })))
                .filter(el -> el.getAmount().compareTo(BigDecimal.ZERO) != 0)
                .sorted(Comparator.comparing(PortfolioAssetResponse::getAssetType)
                        .thenComparing(PortfolioAssetResponse::getAssetTicker))
                .collect(Collectors.toList());
    }

    private Optional<BigDecimal> findPrice(Asset asset, Exchange exchange, Asset priceAsset) {
        // TODO BFS algorithm
        if (BooleanUtils.isNotTrue(Objects.equals(priceAsset.getName(), "CZK"))) {
            return Optional.empty();
        }

        List<Price> assetPrices = assetPriceRepository
                .findByAssetAndExchange(asset, exchange)
                .stream()
                .filter(el -> el.getDate().equals(LocalDate.now().minusDays(1)))
                .collect(Collectors.toList());

        Optional<Price> czkPrice = assetPrices.stream()
                .filter(el -> Objects.nonNull(el.getAsset().getNominalPriceAsset())
                        || Objects.equals(el.getPriceAsset().getName(), "CZK"))
                .findAny();

        if (czkPrice.isPresent()) {

            if (Objects.nonNull(asset.getNominalPriceAsset())) {
                return Optional.of(asset.getNominalPrice()
                        .multiply(czkPrice.get().getPriceValue())
                        .divide(BigDecimal.valueOf(100), 18, RoundingMode.HALF_UP));
            } else {
                return Optional.of(czkPrice.get().getPriceValue());
            }

        } else {

            Optional<Price> usdPrice = assetPrices.stream()
                    .filter(el -> Objects.equals(el.getPriceAsset().getName(), "USD"))
                    .findAny();

            if (usdPrice.isPresent()) {

                Asset usd = assetRepository.findByName("USD").orElseThrow();
                Exchange fio = exchangeRepository.findByAbbreviation(ExchangeAbbrEnum.FIO).orElseThrow();

                Optional<Price> czkUsdPair = assetPriceRepository
                        .findByAssetAndExchange(usd, fio)
                        .stream()
                        .filter(el -> el.getDate().equals(LocalDate.now().minusDays(1)))
                        .filter(el -> Objects.equals(el.getPriceAsset().getName(), "CZK"))
                        .findAny();

                if (czkUsdPair.isPresent()) {
                    return Optional.of(czkUsdPair.get().getPriceValue().multiply(usdPrice.get().getPriceValue()));
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }

        }

    }

    public List<PortfolioAssetPerDay> portfolioPerDay(AppUser appUser) {
        List<PortfolioAssetPerDay> portfolioPerDay = new ArrayList<>();
        List<Ledger> portfolio = ledgerRepository.findByUser(appUser);
        portfolio.stream()
                .map(el -> el.getDate())
                .min(Comparator.comparing(LocalDate::toEpochDay))
                .orElseThrow()
                .datesUntil(LocalDate.now().plusDays(1))
                .forEach(date -> {

                    List<PortfolioAsset> previousAssets = new ArrayList<>();
                    portfolioPerDay.stream()
                            .filter(el -> Objects.equals(el.getDate(), date.minusDays(1)))
                            .map(el -> el.getAssets())
                            .forEach(el -> {
                                el.stream()
                                        .forEach(asset -> {
                                            previousAssets.add(PortfolioAsset.builder()
                                                    .asset(asset.getAsset())
                                                    .amount(asset.getAmount())
                                                    .build());
                                        });

                            });

                    PortfolioAssetPerDay paapd = PortfolioAssetPerDay.builder()
                            .date(date)
                            .assets(previousAssets)
                            .build();

                    portfolio.stream()
                            .filter(el -> Objects.equals(el.getDate(), date))
                            .forEach(asset -> {
                                if (paapd.getAssets().stream()
                                        .anyMatch(
                                                el -> Objects.equals(el.getAsset(), asset.getAsset()))) {
                                    PortfolioAsset paa = paapd.getAssets().stream()
                                            .filter(el -> Objects.equals(el.getAsset(), asset.getAsset()))
                                            .findAny()
                                            .orElseThrow();
                                    paa.setAmount(paa.getAmount().add(asset.getAmount()));
                                } else {
                                    paapd.getAssets().add(PortfolioAsset.builder()
                                            .asset(asset.getAsset())
                                            .amount(asset.getAmount())
                                            .build());
                                }
                            });

                    portfolioPerDay.add(paapd);
                });

        return portfolioPerDay;
    }

}
