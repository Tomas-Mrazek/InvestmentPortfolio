package cz.jaktoviditoka.arion.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.jaktoviditoka.arion.domain.AssetType;
import cz.jaktoviditoka.arion.domain.ExchangeAbbrEnum;
import cz.jaktoviditoka.arion.entity.Asset;
import cz.jaktoviditoka.arion.entity.Exchange;
import cz.jaktoviditoka.arion.entity.Price;
import cz.jaktoviditoka.arion.model.*;
import cz.jaktoviditoka.arion.repository.AssetRepository;
import cz.jaktoviditoka.arion.repository.ExchangeRepository;
import cz.jaktoviditoka.arion.repository.PriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@Component
public class ImportService {

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    PriceRepository priceRepository;

    @Autowired
    KurzyCzClient kurzyCzClient;

    @Autowired
    FioForexClient fioForexClient;
    
    @Autowired
    AlphaVantageClient alphaVantageClient;

    @Autowired
    ObjectMapper objectMapper;

    private static final LocalDate BCPP_INIT_DATE = LocalDate.of(2001, 10, 1);
    private static final LocalDate FIO_FOREX_INIT_DATE = LocalDate.of(2003, 01, 02);
    private static final String PRICE_ASSET = "CZK";

    @Transactional
    public void importCurrencies() {
        Set<Currency> currencies = Currency.getAvailableCurrencies();
        List<Asset> assets = currencies.stream()
                .map(map -> {
                    return Asset.builder()
                            .name(map.getDisplayName(Locale.ENGLISH))
                            .ticker(map.getCurrencyCode())
                            .type(AssetType.CURRENCY)
                            .build();
                })
                .collect(Collectors.toList());
        log.debug("Saving {} currencies to database...", assets.size());
        assetRepository.saveAll(assets);
        log.debug("Done...");
    }

    /************/
    /* KURZY CZ */
    /************/
    
    @Transactional
    public void importKurzyCzToFile(Exchange exchange) throws IOException, InterruptedException {
        LocalDate from = BCPP_INIT_DATE;
        LocalDate to = LocalDate.now();
        importKurzyCzToFile(exchange, from, to);
    }

    @Transactional
    public void importKurzyCzToFile(Exchange exchange, LocalDate from, LocalDate to)
            throws IOException, InterruptedException {
        String fileName = "KurzyCZ_" + exchange.getAbbreviation() + "_"
                + from.format(DateTimeFormatter.ISO_DATE) + "_"
                + to.format(DateTimeFormatter.ISO_DATE) + ".json";
        File file = new File(fileName);
        if (!file.exists()) {
            List<LocalDate> dates = from.datesUntil(to.plusDays(1)).collect(Collectors.toList());
            List<KurzyCzPrice> prices = new ArrayList<>();
            for (LocalDate date : dates) {
                prices.add(kurzyCzClient.importPrice(exchange, date));
                Thread.sleep(new Random().nextInt(10) + 50l);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, prices);
        }
    }

    @Transactional
    public void importAssetFromKurzyCzFile(Exchange exchange, LocalDate from, LocalDate to) throws IOException {
        File file = new File("KurzyCZ_" + exchange.getAbbreviation().name() + "_"
                + from.format(DateTimeFormatter.ISO_DATE) + "_"
                + to.format(DateTimeFormatter.ISO_DATE) + ".json");
        if (file.exists()) {
            List<KurzyCzPrice> prices = objectMapper.readValue(file, new TypeReference<List<KurzyCzPrice>>() {
            });
            List<Asset> assets = prices.stream()
                    .map(map -> map.getListOfPrices())
                    .flatMap(Collection::stream)
                    .map(map -> {
                        String ticker = hyphenToNull(map.getBic());

                        AssetType type;
                        if (map.getBic().startsWith("BA") || map.getBic().startsWith("BF")) {
                            type = AssetType.STOCK;
                        } else if (map.getBic().startsWith("BC") || map.getBic().startsWith("BD")
                                || map.getBic().startsWith("BE") || map.getBic().startsWith("BG")
                                || map.getBic().startsWith("BO")) {
                            type = AssetType.BOND;
                        } else if (map.getBic().startsWith("BL")) {
                            type = AssetType.SHARE;
                        } else {
                            type = AssetType.UNKNOWN;
                        }
                        return Asset.builder()
                                .name(map.getName())
                                .ticker(ticker)
                                .isin(map.getIsin())
                                .type(type)
                                .build();
                    })
                    .distinct()
                    .collect(Collectors.toList());
            log.debug("Saving {} assets from KurzyCZ to database...", assets.size());
            assetRepository.saveAll(assets);
            log.debug("Done...");
        }
    }
    
    @Transactional
    public void importPriceFromKurzyCzFile(Exchange exchange, LocalDate from, LocalDate to) throws IOException {
        File file = new File("KurzyCZ_" + exchange.getAbbreviation().name() + "_"
                + from.format(DateTimeFormatter.ISO_DATE) + "_"
                + to.format(DateTimeFormatter.ISO_DATE) + ".json");
        if (file.exists()) {
            List<Asset> assets = assetRepository.findAll();
            Asset priceAsset = assets.stream()
                    .filter(el -> Objects.equals(el.getTicker(), PRICE_ASSET))
                    .findAny()
                    .orElseThrow();

            List<KurzyCzPrice> kurzyCzPrices = objectMapper.readValue(file, new TypeReference<List<KurzyCzPrice>>() {
            });
            List<Price> prices = kurzyCzPrices.stream()
                    .map(map -> map.getListOfPrices())
                    .flatMap(Collection::stream)
                    .map(map -> {
                        Optional<Asset> assetOpt = assets.stream()
                                .filter(el -> Objects.equals(el.getIsin(), map.getIsin()))
                                .findAny();
                        if (assetOpt.isEmpty()) {
                            log.debug("ISIN not found. -> {}", map);
                            return null;
                        } else {
                            return Price.builder()
                                    .date(LocalDate.parse(map.getTradingDay(), DateTimeFormatter.ofPattern("d.M.yyyy")))
                                    .asset(assetOpt.get())
                                    .exchange(exchange)
                                    .priceAsset(priceAsset)
                                    .openingPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceOpen())))
                                    .lowPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceMinDay())))
                                    .highPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceMaxDay())))
                                    .closingPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceClose())))
                                    .priceChange(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceChange())))
                                    .volume(NumberUtils.createBigDecimal(hyphenToNull(map.getTradeShares())))
                                    .turnover(NumberUtils.createBigDecimal(hyphenToNull(map.getTradeVolume())))
                                    .build();
                        }
                    })
                    .distinct()
                    .collect(Collectors.toList());
            log.debug("Saving {} prices from KurzyCZ to database...", prices.size());
            priceRepository.saveAll(prices);
            log.debug("Done...");
        }
    }
    
    @Transactional
    public void importAssetFromKurzyCz(Exchange exchange, LocalDate date) throws IOException {
        KurzyCzPrice prices = kurzyCzClient.importPrice(exchange, date);
        List<Asset> assets = prices.getListOfPrices().stream()
                .map(map -> {
                    String ticker = hyphenToNull(map.getBic());

                    AssetType type;
                    if (map.getBic().startsWith("BA") || map.getBic().startsWith("BF")) {
                        type = AssetType.STOCK;
                    } else if (map.getBic().startsWith("BC") || map.getBic().startsWith("BD")
                            || map.getBic().startsWith("BE") || map.getBic().startsWith("BG")
                            || map.getBic().startsWith("BO")) {
                        type = AssetType.BOND;
                    } else if (map.getBic().startsWith("BL")) {
                        type = AssetType.SHARE;
                    } else {
                        type = AssetType.UNKNOWN;
                    }
                    return Asset.builder()
                            .name(map.getName())
                            .ticker(ticker)
                            .isin(map.getIsin())
                            .type(type)
                            .build();
                })
                .distinct()
                .collect(Collectors.toList());
        log.debug("Saving {} assets from KurzyCZ to database...", assets.size());
        assetRepository.saveAll(assets);
        log.debug("Done...");
    }

    @Transactional
    public void importPriceFromKurzyCz(Exchange exchange, LocalDate date) throws IOException {
        List<Asset> assets = assetRepository.findAll();
        Asset priceAsset = assets.stream()
                .filter(el -> Objects.equals(el.getTicker(), PRICE_ASSET))
                .findAny()
                .orElseThrow();

        KurzyCzPrice kurzyCzPrices = kurzyCzClient.importPrice(exchange, date);

        List<Price> prices = kurzyCzPrices.getListOfPrices().stream()
                .map(map -> {
                    Optional<Asset> assetOpt = assets.stream()
                            .filter(el -> Objects.equals(el.getIsin(), map.getIsin()))
                            .findAny();
                    if (assetOpt.isEmpty()) {
                        log.debug("ISIN not found. -> {}", map);
                        return null;
                    } else {
                        return Price.builder()
                                .date(LocalDate.parse(map.getTradingDay(), DateTimeFormatter.ofPattern("d.M.yyyy")))
                                .asset(assetOpt.get())
                                .exchange(exchange)
                                .priceAsset(priceAsset)
                                .openingPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceOpen())))
                                .lowPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceMinDay())))
                                .highPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceMaxDay())))
                                .closingPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceClose())))
                                .priceChange(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceChange())))
                                .volume(NumberUtils.createBigDecimal(hyphenToNull(map.getTradeShares())))
                                .turnover(NumberUtils.createBigDecimal(hyphenToNull(map.getTradeVolume())))
                                .build();
                    }
                })
                .distinct()
                .collect(Collectors.toList());
        log.debug("Saving {} prices from KurzyCZ to database...", prices.size());
        priceRepository.saveAll(prices);
        log.debug("Done...");
    }

    /*************/
    /* FIO FOREX */
    /*************/
    
    @Transactional
    public void importFioForexToFile() throws IOException, InterruptedException {
        LocalDate from = FIO_FOREX_INIT_DATE;
        LocalDate to = LocalDate.now();
        importFioForexToFile(from, to);
    }    
    
    @Transactional
    public void importFioForexToFile(LocalDate from, LocalDate to) throws IOException, InterruptedException {
        String fileName = "Fio_Forex_" + from.format(DateTimeFormatter.ISO_DATE) + "_"
                + to.format(DateTimeFormatter.ISO_DATE) + ".json";
        File file = new File(fileName);
        if (!file.exists()) {
            List<LocalDate> dates = from.datesUntil(to.plusDays(1)).collect(Collectors.toList());
            List<List<FioForexPrice>> prices = new ArrayList<>();
            for (LocalDate date : dates) {
                prices.add(fioForexClient.importPrice(date));
                Thread.sleep(new Random().nextInt(10) + 50l);
            }
            List<FioForexPrice> flattenPrices = prices.stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, flattenPrices);
        }
    }

    @Transactional
    public void importPriceFromFioForexFile(LocalDate from, LocalDate to) throws IOException {
        String fileName = "Fio_Forex_" + from.format(DateTimeFormatter.ISO_DATE) + "_"
                + to.format(DateTimeFormatter.ISO_DATE) + ".json";
        File file = new File(fileName);
        if (file.exists()) {
            List<Asset> assets = assetRepository.findAll();

            Exchange exchange = exchangeRepository.findByAbbreviation(ExchangeAbbrEnum.FIO).orElseThrow();

            List<FioForexPrice> fioForexPrices = objectMapper.readValue(file, new TypeReference<List<FioForexPrice>>() {
            });
            List<Price> prices = fioForexPrices.stream()
                    .map(map -> {
                        Optional<Asset> assetOpt = assets.stream()
                                .filter(el -> Objects.equals(el.getTicker(), map.getAsset()))
                                .findAny();
                        Optional<Asset> priceAssetOpt = assets.stream()
                                .filter(el -> Objects.equals(el.getTicker(), PRICE_ASSET))
                                .findAny();
                        if (assetOpt.isEmpty() || priceAssetOpt.isEmpty()) {
                            log.debug("Currency not found. -> {}", map);
                            return null;
                        } else {
                            List<Price> forexPrice = new ArrayList<>();

                            BigDecimal amount = NumberUtils.createBigDecimal(map.getAmount().replace(',', '.'));

                            Price buy = Price.builder()
                                    .date(LocalDate.parse(map.getDate(), DateTimeFormatter.ISO_DATE))
                                    .asset(priceAssetOpt.get())
                                    .exchange(exchange)
                                    .priceAsset(assetOpt.get())
                                    .openingPrice(null)
                                    .lowPrice(null)
                                    .highPrice(null)
                                    .closingPrice(BigDecimal.ONE
                                            .divide(NumberUtils.createBigDecimal(map.getBuyPrice().replace(',', '.'))
                                                    .divide(amount), 5, RoundingMode.HALF_EVEN))
                                    .priceChange(null)
                                    .volume(null)
                                    .turnover(null)
                                    .build();
                            forexPrice.add(buy);

                            Price sell = Price.builder()
                                    .date(LocalDate.parse(map.getDate(), DateTimeFormatter.ISO_DATE))
                                    .asset(assetOpt.get())
                                    .exchange(exchange)
                                    .priceAsset(priceAssetOpt.get())
                                    .openingPrice(null)
                                    .lowPrice(null)
                                    .highPrice(null)
                                    .closingPrice(NumberUtils.createBigDecimal(map.getSellPrice().replace(',', '.'))
                                            .divide(amount))
                                    .priceChange(null)
                                    .volume(null)
                                    .turnover(null)
                                    .build();
                            forexPrice.add(sell);

                            return forexPrice;
                        }
                    })
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList());
            log.debug("Saving {} prices from Fio e-Broker to database...", prices.size());
            priceRepository.saveAll(prices);
            log.debug("Done...");
        }
    }
    
    @Transactional
    public void importPriceFromFioForex(LocalDate date) throws IOException {
            List<Asset> assets = assetRepository.findAll();

            Exchange exchange = exchangeRepository.findByAbbreviation(ExchangeAbbrEnum.FIO).orElseThrow();

            List<FioForexPrice> fioForexPrices = fioForexClient.importPrice(date);
            
            List<Price> prices = fioForexPrices.stream()
                    .map(map -> {
                        Optional<Asset> assetOpt = assets.stream()
                                .filter(el -> Objects.equals(el.getTicker(), map.getAsset()))
                                .findAny();
                        Optional<Asset> priceAssetOpt = assets.stream()
                                .filter(el -> Objects.equals(el.getTicker(), PRICE_ASSET))
                                .findAny();
                        if (assetOpt.isEmpty() || priceAssetOpt.isEmpty()) {
                            log.debug("Currency not found. -> {}", map);
                            return null;
                        } else {
                            List<Price> forexPrice = new ArrayList<>();

                            BigDecimal amount = NumberUtils.createBigDecimal(map.getAmount().replace(',', '.'));

                            Price buy = Price.builder()
                                    .date(LocalDate.parse(map.getDate(), DateTimeFormatter.ISO_DATE))
                                    .asset(priceAssetOpt.get())
                                    .exchange(exchange)
                                    .priceAsset(assetOpt.get())
                                    .openingPrice(null)
                                    .lowPrice(null)
                                    .highPrice(null)
                                    .closingPrice(BigDecimal.ONE
                                            .divide(NumberUtils.createBigDecimal(map.getBuyPrice().replace(',', '.'))
                                                    .divide(amount), 5, RoundingMode.HALF_EVEN))
                                    .priceChange(null)
                                    .volume(null)
                                    .turnover(null)
                                    .build();
                            forexPrice.add(buy);

                            Price sell = Price.builder()
                                    .date(LocalDate.parse(map.getDate(), DateTimeFormatter.ISO_DATE))
                                    .asset(assetOpt.get())
                                    .exchange(exchange)
                                    .priceAsset(priceAssetOpt.get())
                                    .openingPrice(null)
                                    .lowPrice(null)
                                    .highPrice(null)
                                    .closingPrice(NumberUtils.createBigDecimal(map.getSellPrice().replace(',', '.'))
                                            .divide(amount))
                                    .priceChange(null)
                                    .volume(null)
                                    .turnover(null)
                                    .build();
                            forexPrice.add(sell);

                            return forexPrice;
                        }
                    })
                    .flatMap(Collection::stream)
                    .distinct()
                    .collect(Collectors.toList());
            log.debug("Saving {} prices from Fio e-Broker to database...", prices.size());
            priceRepository.saveAll(prices);
            log.debug("Done...");
    }
    
    /****************/
    /* AlphaVantage */
    /****************/
    
    @Transactional
    public void importPriceFromAlphaVantage(String ticker) throws IOException {
        alphaVantageClient.getAssetHistoricPrice(ticker);
    }

    /*********/
    /* UTILS */
    /*********/
    
    private String hyphenToNull(String value) {
        if (value.equals("-")) {
            return null;
        } else {
            return value;
        }
    }

}
