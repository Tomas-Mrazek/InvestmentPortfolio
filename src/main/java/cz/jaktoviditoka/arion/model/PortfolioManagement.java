package cz.jaktoviditoka.arion.model;

import com.google.common.collect.MoreCollectors;
import cz.jaktoviditoka.arion.domain.AssetType;
import cz.jaktoviditoka.arion.domain.PortfolioAsset;
import cz.jaktoviditoka.arion.domain.PortfolioAssetPerDay;
import cz.jaktoviditoka.arion.domain.TransactionType;
import cz.jaktoviditoka.arion.dto.PortfolioDayValueDto;
import cz.jaktoviditoka.arion.dto.PortfolioHistoryDay;
import cz.jaktoviditoka.arion.entity.AppUser;
import cz.jaktoviditoka.arion.entity.Asset;
import cz.jaktoviditoka.arion.entity.Ledger;
import cz.jaktoviditoka.arion.entity.Price;
import cz.jaktoviditoka.arion.repository.AssetRepository;
import cz.jaktoviditoka.arion.repository.ExchangeRepository;
import cz.jaktoviditoka.arion.repository.LedgerRepository;
import cz.jaktoviditoka.arion.repository.PriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
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
    PriceRepository priceRepository;

    @Autowired
    ModelMapper modelMapper;

    private static final String PRICE_ASSET = "CZK";

    public List<PortfolioAssetPerDay> portfolioPerDay(AppUser appUser) {
        List<PortfolioAssetPerDay> portfolioPerDay = new ArrayList<>();
        List<Ledger> portfolio = ledgerRepository.findByUser(appUser);
        portfolio.stream()
                .map(el -> el.getTimestamp().toLocalDate())
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
                            .filter(el -> Objects.equals(el.getTimestamp().toLocalDate(), date))
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

    public List<PortfolioHistoryDay> portfolioHistory(AppUser appUser) {
        log.trace("PortfolioHistory – Started...");
        List<Ledger> ledger = ledgerRepository.findByUser(appUser);
        Asset baseAsset = assetRepository.findByTicker(PRICE_ASSET).orElseThrow();

        // Find first ledger date
        log.trace("PortfolioHistory – Find first ledger date...");
        LocalDate startDate = ledger.stream()
                .map(el -> el.getTimestamp().toLocalDate())
                .min(Comparator.comparing(LocalDate::toEpochDay))
                .orElseThrow();

        // Create list of all dates between first ledger date and today
        log.trace("PortfolioHistory – Create list of dates...");
        List<LocalDate> dates = startDate
                .datesUntil(LocalDate.now().plusDays(1))
                .collect(Collectors.toList());

        // Create list of portfolio day with zero amount and value for each asset from
        // ledger
        log.trace("PortfolioHistory – Create empty day portfolio list...");
        List<PortfolioHistoryDay> dayPortfolioList = ledger.stream()
                .map(ledgerEntry -> ledgerEntry.getAsset())
                .distinct()
                .map(asset -> {
                    return new PortfolioHistoryDay(startDate, asset, BigDecimal.ZERO, BigDecimal.ZERO);
                })
                .collect(Collectors.toList());

        for (LocalDate date : dates) {
            log.trace("PortfolioHistory – Processing date " + date + " ...");

            // Find ledger entries for specific date
            List<Ledger> ledgerEntries = ledger.stream()
                    .filter(el -> Objects.equals(el.getTimestamp().toLocalDate(), date))
                    .collect(Collectors.toList());

            // Process ledger entries
            log.trace("PortfolioHistory – Process ledger entries...");
            ledgerEntries.stream()
                    .forEach(ledgerEntry -> {

                        // Find existing portfolio day for specific asset and date
                        PortfolioHistoryDay portfolioHistoryDay = dayPortfolioList.stream()
                                .filter(dayPortfolio -> Objects.equals(dayPortfolio.getDate(), date))
                                .filter(dayPortfolio -> Objects.equals(dayPortfolio.getAsset(), ledgerEntry.getAsset()))
                                .collect(MoreCollectors.onlyElement());

                        // Update amount from ledger entry
                        portfolioHistoryDay.setAmount(portfolioHistoryDay.getAmount().add(ledgerEntry.getAmount()));
                    });

            // Calculate value
            log.trace("PortfolioHistory – Calculate value...");
            List<Price> pricesDay = priceRepository.findByDate(date);
            dayPortfolioList.stream()
                    .filter(dayPortfolio -> Objects.equals(dayPortfolio.getDate(), date))
                    .forEach(dayPortfolio -> {
                        Asset asset = dayPortfolio.getAsset();
                        if (Objects.equals(asset, baseAsset)) {
                            dayPortfolio.setValue(dayPortfolio.getAmount());
                        } else {
                            // Find any price for specific asset
                            List<Price> prices = pricesDay.stream()
                                    .filter(price -> Objects.equals(price.getAsset(), asset))
                                    .collect(Collectors.toList());
                            if (prices.isEmpty()) {
                                log.warn("PortfolioHistory - No price found for asset {} and date {}.",
                                        dayPortfolio.getAsset().getTicker(),
                                        dayPortfolio.getDate());
                            } else {
                                // Find base currency price for specific asset
                                Optional<Price> basePriceOpt = prices.stream()
                                        .filter(price -> Objects.equals(price.getAsset(), asset))
                                        .filter(price -> Objects.equals(price.getPriceAsset(), baseAsset))
                                        .findAny();
                                if (basePriceOpt.isEmpty()) {
                                    // Find any currency for specific asset
                                    Optional<Price> anyPriceOpt = prices.stream()
                                            .filter(price -> Objects.equals(price.getAsset(), asset))
                                            .findAny();
                                    if (anyPriceOpt.isEmpty()) {
                                        log.warn("PortfolioHistory - No price found for asset {} and date {}.",
                                                dayPortfolio.getAsset().getTicker(),
                                                dayPortfolio.getDate());
                                    } else {
                                        // Find price for any currency and base currency
                                        Optional<Price> convertPriceOpt = pricesDay.stream()
                                                .filter(price -> Objects.equals(price.getAsset(),
                                                        anyPriceOpt.get().getPriceAsset()))
                                                .filter(price -> Objects.equals(price.getPriceAsset(), baseAsset))
                                                .findAny();
                                        if (convertPriceOpt.isEmpty()) {
                                            log.warn(
                                                    "PortfolioHistory - No convert price found for asset {} and date {}.",
                                                    dayPortfolio.getAsset().getTicker(),
                                                    dayPortfolio.getDate());
                                        } else {
                                            if (Objects.equals(dayPortfolio.getAsset().getType(), AssetType.BOND)) {
                                                dayPortfolio.setValue(dayPortfolio.getAmount()
                                                        .multiply(anyPriceOpt.get().getClosingPrice())
                                                        .multiply(convertPriceOpt.get().getClosingPrice())
                                                        .multiply(dayPortfolio.getAsset().getNominalPrice())
                                                        .divide(BigDecimal.valueOf(100))
                                                        .setScale(18, RoundingMode.HALF_EVEN));
                                            } else {
                                                dayPortfolio.setValue(dayPortfolio.getAmount()
                                                        .multiply(anyPriceOpt.get().getClosingPrice())
                                                        .multiply(convertPriceOpt.get().getClosingPrice())
                                                        .setScale(18, RoundingMode.HALF_EVEN));
                                            }
                                        }
                                    }
                                } else {
                                    if (Objects.equals(dayPortfolio.getAsset().getType(), AssetType.BOND)) {
                                        dayPortfolio.setValue(dayPortfolio.getAmount()
                                                .multiply(basePriceOpt.get().getClosingPrice())
                                                .multiply(dayPortfolio.getAsset().getNominalPrice())
                                                .divide(BigDecimal.valueOf(100))
                                                .setScale(18, RoundingMode.HALF_EVEN));
                                    } else {
                                        dayPortfolio.setValue(dayPortfolio.getAmount()
                                                .multiply(basePriceOpt.get().getClosingPrice())
                                                .setScale(18, RoundingMode.HALF_EVEN));
                                    }
                                }
                            }
                        }

                    });

            // Create next day portfolio from updated current day portfolio
            log.trace("PortfolioHistory – Create next day portfolio...");
            List<PortfolioHistoryDay> dayPortfolioListNew = new ArrayList<>();
            dayPortfolioList.stream()
                    .filter(dayPortfolio -> Objects.equals(dayPortfolio.getDate(), date))
                    .forEach(dayPortfolio -> {
                        PortfolioHistoryDay nextDayPortfolio = new PortfolioHistoryDay(
                                date.plusDays(1),
                                dayPortfolio.getAsset(),
                                NumberUtils.createBigDecimal(dayPortfolio.getAmount().toString()),
                                NumberUtils.createBigDecimal(dayPortfolio.getValue().toString()));
                        dayPortfolioListNew.add(nextDayPortfolio);
                    });
            dayPortfolioList.addAll(dayPortfolioListNew);
        }

        log.trace("PortfolioHistory – Done...");
        return dayPortfolioList;
    }
    
    public List<PortfolioHistoryDay> portfolioHistoryEarnings(AppUser appUser) {
        log.trace("PortfolioHistoryEarnings – Started...");
        List<Ledger> ledger = ledgerRepository.findByUser(appUser).stream()
                .filter(ledgerEntry -> BooleanUtils.isFalse(Objects.equals(ledgerEntry.getTransaction().getType(), TransactionType.DEPOSIT)))
                .filter(ledgerEntry -> BooleanUtils.isFalse(Objects.equals(ledgerEntry.getTransaction().getType(), TransactionType.TRANSFER)))
                .filter(ledgerEntry -> BooleanUtils.isFalse(Objects.equals(ledgerEntry.getTransaction().getType(), TransactionType.WITHDRAWAL)))
                .collect(Collectors.toList());
        Asset baseAsset = assetRepository.findByTicker(PRICE_ASSET).orElseThrow();

        // Find first ledger date
        log.trace("PortfolioHistoryEarnings – Find first ledger date...");
        LocalDate startDate = ledger.stream()
                .map(el -> el.getTimestamp().toLocalDate())
                .min(Comparator.comparing(LocalDate::toEpochDay))
                .orElseThrow();

        // Create list of all dates between first ledger date and today
        log.trace("PortfolioHistoryEarnings – Create list of dates...");
        List<LocalDate> dates = startDate
                .datesUntil(LocalDate.now().plusDays(1))
                .collect(Collectors.toList());

        // Create list of portfolio day with zero amount and value for each asset from
        // ledger
        log.trace("PortfolioHistoryEarnings – Create empty day portfolio list...");
        List<PortfolioHistoryDay> dayPortfolioList = ledger.stream()
                .map(ledgerEntry -> ledgerEntry.getAsset())
                .distinct()
                .map(asset -> {
                    return new PortfolioHistoryDay(startDate, asset, BigDecimal.ZERO, BigDecimal.ZERO);
                })
                .collect(Collectors.toList());

        for (LocalDate date : dates) {
            log.trace("PortfolioHistoryEarnings – Processing date " + date + " ...");

            // Find ledger entries for specific date
            List<Ledger> ledgerEntries = ledger.stream()
                    .filter(el -> Objects.equals(el.getTimestamp().toLocalDate(), date))
                    .collect(Collectors.toList());

            // Process ledger entries
            log.trace("PortfolioHistoryEarnings – Process ledger entries...");
            ledgerEntries.stream()
                    .forEach(ledgerEntry -> {

                        // Find existing portfolio day for specific asset and date
                        PortfolioHistoryDay portfolioHistoryDay = dayPortfolioList.stream()
                                .filter(dayPortfolio -> Objects.equals(dayPortfolio.getDate(), date))
                                .filter(dayPortfolio -> Objects.equals(dayPortfolio.getAsset(), ledgerEntry.getAsset()))
                                .collect(MoreCollectors.onlyElement());

                        // Update amount from ledger entry
                        portfolioHistoryDay.setAmount(portfolioHistoryDay.getAmount().add(ledgerEntry.getAmount()));
                    });

            // Calculate value
            log.trace("PortfolioHistoryEarnings – Calculate value...");
            List<Price> pricesDay = priceRepository.findByDate(date);
            dayPortfolioList.stream()
                    .filter(dayPortfolio -> Objects.equals(dayPortfolio.getDate(), date))
                    .forEach(dayPortfolio -> {
                        Asset asset = dayPortfolio.getAsset();
                        if (Objects.equals(asset, baseAsset)) {
                            dayPortfolio.setValue(dayPortfolio.getAmount());
                        } else {
                            // Find any price for specific asset
                            List<Price> prices = pricesDay.stream()
                                    .filter(price -> Objects.equals(price.getAsset(), asset))
                                    .collect(Collectors.toList());
                            if (prices.isEmpty()) {
                                log.warn("PortfolioHistoryEarnings - No price found for asset {} and date {}.",
                                        dayPortfolio.getAsset().getTicker(),
                                        dayPortfolio.getDate());
                            } else {
                                // Find base currency price for specific asset
                                Optional<Price> basePriceOpt = prices.stream()
                                        .filter(price -> Objects.equals(price.getAsset(), asset))
                                        .filter(price -> Objects.equals(price.getPriceAsset(), baseAsset))
                                        .findAny();
                                if (basePriceOpt.isEmpty()) {
                                    // Find any currency for specific asset
                                    Optional<Price> anyPriceOpt = prices.stream()
                                            .filter(price -> Objects.equals(price.getAsset(), asset))
                                            .findAny();
                                    if (anyPriceOpt.isEmpty()) {
                                        log.warn("PortfolioHistoryEarnings - No price found for asset {} and date {}.",
                                                dayPortfolio.getAsset().getTicker(),
                                                dayPortfolio.getDate());
                                    } else {
                                        // Find price for any currency and base currency
                                        Optional<Price> convertPriceOpt = pricesDay.stream()
                                                .filter(price -> Objects.equals(price.getAsset(),
                                                        anyPriceOpt.get().getPriceAsset()))
                                                .filter(price -> Objects.equals(price.getPriceAsset(), baseAsset))
                                                .findAny();
                                        if (convertPriceOpt.isEmpty()) {
                                            log.warn(
                                                    "PortfolioHistoryEarnings - No convert price found for asset {} and date {}.",
                                                    dayPortfolio.getAsset().getTicker(),
                                                    dayPortfolio.getDate());
                                        } else {
                                            if (Objects.equals(dayPortfolio.getAsset().getType(), AssetType.BOND)) {
                                                dayPortfolio.setValue(dayPortfolio.getAmount()
                                                        .multiply(anyPriceOpt.get().getClosingPrice())
                                                        .multiply(convertPriceOpt.get().getClosingPrice())
                                                        .multiply(dayPortfolio.getAsset().getNominalPrice())
                                                        .divide(BigDecimal.valueOf(100))
                                                        .setScale(18, RoundingMode.HALF_EVEN));
                                            } else {
                                                dayPortfolio.setValue(dayPortfolio.getAmount()
                                                        .multiply(anyPriceOpt.get().getClosingPrice())
                                                        .multiply(convertPriceOpt.get().getClosingPrice())
                                                        .setScale(18, RoundingMode.HALF_EVEN));
                                            }
                                        }
                                    }
                                } else {
                                    if (Objects.equals(dayPortfolio.getAsset().getType(), AssetType.BOND)) {
                                        dayPortfolio.setValue(dayPortfolio.getAmount()
                                                .multiply(basePriceOpt.get().getClosingPrice())
                                                .multiply(dayPortfolio.getAsset().getNominalPrice())
                                                .divide(BigDecimal.valueOf(100))
                                                .setScale(18, RoundingMode.HALF_EVEN));
                                    } else {
                                        dayPortfolio.setValue(dayPortfolio.getAmount()
                                                .multiply(basePriceOpt.get().getClosingPrice())
                                                .setScale(18, RoundingMode.HALF_EVEN));
                                    }
                                }
                            }
                        }

                    });

            // Create next day portfolio from updated current day portfolio
            log.trace("PortfolioHistoryEarnings – Create next day portfolio...");
            List<PortfolioHistoryDay> dayPortfolioListNew = new ArrayList<>();
            dayPortfolioList.stream()
                    .filter(dayPortfolio -> Objects.equals(dayPortfolio.getDate(), date))
                    .forEach(dayPortfolio -> {
                        PortfolioHistoryDay nextDayPortfolio = new PortfolioHistoryDay(
                                date.plusDays(1),
                                dayPortfolio.getAsset(),
                                NumberUtils.createBigDecimal(dayPortfolio.getAmount().toString()),
                                NumberUtils.createBigDecimal(dayPortfolio.getValue().toString()));
                        dayPortfolioListNew.add(nextDayPortfolio);
                    });
            dayPortfolioList.addAll(dayPortfolioListNew);
        }

        log.trace("PortfolioHistoryEarnings – Done...");
        return dayPortfolioList;
    }

    public List<PortfolioDayValueDto> portfolioValue(AppUser appUser) {
        List<PortfolioHistoryDay> portfolioHistory = portfolioHistory(appUser);
        return portfolioHistory.stream()
                .collect(Collectors.groupingBy(PortfolioHistoryDay::getDate,
                        Collectors.reducing(BigDecimal.ZERO, PortfolioHistoryDay::getValue, BigDecimal::add)))
                .entrySet()
                .stream()
                .map(map -> PortfolioDayValueDto.builder()
                        .date(map.getKey())
                        .value(map.getValue())
                        .build())
                .sorted((o1, o2) -> o1.getDate().compareTo(o2.getDate()))
                .collect(Collectors.toList());
    }
    
    public List<PortfolioDayValueDto> portfolioEarnings(AppUser appUser) {
        List<PortfolioHistoryDay> portfolioHistory = portfolioHistoryEarnings(appUser);
        return portfolioHistory.stream()
                .collect(Collectors.groupingBy(PortfolioHistoryDay::getDate,
                        Collectors.reducing(BigDecimal.ZERO, PortfolioHistoryDay::getValue, BigDecimal::add)))
                .entrySet()
                .stream()
                .map(map -> PortfolioDayValueDto.builder()
                        .date(map.getKey())
                        .value(map.getValue())
                        .build())
                .sorted((o1, o2) -> o1.getDate().compareTo(o2.getDate()))
                .collect(Collectors.toList());
    }

}
