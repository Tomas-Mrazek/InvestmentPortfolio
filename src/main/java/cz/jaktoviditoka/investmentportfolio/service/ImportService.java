package cz.jaktoviditoka.investmentportfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.jaktoviditoka.investmentportfolio.domain.AssetType;
import cz.jaktoviditoka.investmentportfolio.domain.ExchangeAbbrEnum;
import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.entity.Price;
import cz.jaktoviditoka.investmentportfolio.model.*;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.ExchangeRepository;
import cz.jaktoviditoka.investmentportfolio.repository.PriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
        assetRepository.saveAll(assets);
    }

    /************/
    /* KURZY CZ */
    /************/
    
    public void importKurzyCzToFile(Exchange exchange) throws IOException, InterruptedException {
        LocalDate from = BCPP_INIT_DATE;
        LocalDate to = LocalDate.now();
        importKurzyCzToFile(exchange, from, to);
    }

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
            log.debug("Saving assets to database...");
            assetRepository.saveAll(assets);
        }
    }
    

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
                                    .closingPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceClose())))
                                    .priceChange(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceChange())))
                                    .minPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceMinDay())))
                                    .maxPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceMaxDay())))
                                    .volume(NumberUtils.createBigDecimal(hyphenToNull(map.getTradeShares())))
                                    .turnover(NumberUtils.createBigDecimal(hyphenToNull(map.getTradeVolume())))
                                    .build();
                        }
                    })
                    .distinct()
                    .collect(Collectors.toList());
            log.debug("Saving prices to database...");
            priceRepository.saveAll(prices);
        }
    }
    
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
        log.debug("Saving assets to database...");
        assetRepository.saveAll(assets);
    }

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
                                .closingPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceClose())))
                                .priceChange(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceChange())))
                                .minPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceMinDay())))
                                .maxPrice(NumberUtils.createBigDecimal(hyphenToNull(map.getPriceMaxDay())))
                                .volume(NumberUtils.createBigDecimal(hyphenToNull(map.getTradeShares())))
                                .turnover(NumberUtils.createBigDecimal(hyphenToNull(map.getTradeVolume())))
                                .build();
                    }
                })
                .distinct()
                .collect(Collectors.toList());
        log.debug("Saving prices to database...");
        priceRepository.saveAll(prices);
    }

    /*************/
    /* FIO FOREX */
    /*************/
    
    public void importFioForexToFile() throws IOException, InterruptedException {
        LocalDate from = FIO_FOREX_INIT_DATE;
        LocalDate to = LocalDate.now();
        importFioForexToFile(from, to);
    }    
    
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
                                    .closingPrice(BigDecimal.ONE
                                            .divide(NumberUtils.createBigDecimal(map.getBuyPrice().replace(',', '.'))
                                                    .divide(amount), 5, RoundingMode.HALF_EVEN))
                                    .priceChange(null)
                                    .minPrice(null)
                                    .maxPrice(null)
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
                                    .closingPrice(NumberUtils.createBigDecimal(map.getSellPrice().replace(',', '.'))
                                            .divide(amount))
                                    .priceChange(null)
                                    .minPrice(null)
                                    .maxPrice(null)
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
            log.debug("Saving prices to database...");
            priceRepository.saveAll(prices);
        }
    }
    
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
                                    .closingPrice(BigDecimal.ONE
                                            .divide(NumberUtils.createBigDecimal(map.getBuyPrice().replace(',', '.'))
                                                    .divide(amount), 5, RoundingMode.HALF_EVEN))
                                    .priceChange(null)
                                    .minPrice(null)
                                    .maxPrice(null)
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
                                    .closingPrice(NumberUtils.createBigDecimal(map.getSellPrice().replace(',', '.'))
                                            .divide(amount))
                                    .priceChange(null)
                                    .minPrice(null)
                                    .maxPrice(null)
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
            log.debug("Saving prices to database...");
            priceRepository.saveAll(prices);
    }
    
    /****************/
    /* AlphaVantage */
    /****************/
    
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
