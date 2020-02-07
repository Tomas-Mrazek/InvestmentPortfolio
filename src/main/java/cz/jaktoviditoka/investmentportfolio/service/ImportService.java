package cz.jaktoviditoka.investmentportfolio.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.jaktoviditoka.investmentportfolio.domain.AssetType;
import cz.jaktoviditoka.investmentportfolio.domain.ExchangeAbbrEnum;
import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.entity.Price;
import cz.jaktoviditoka.investmentportfolio.model.KurzyCzClient;
import cz.jaktoviditoka.investmentportfolio.model.KurzyCzPrice;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.ExchangeRepository;
import cz.jaktoviditoka.investmentportfolio.repository.PriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
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

    private static final LocalDate BCPP_INIT_DATE = LocalDate.of(2001, 10, 1);

    public void importKurzyCzToFile(Exchange exchange) throws IOException, InterruptedException {
        LocalDate from = BCPP_INIT_DATE;
        LocalDate to = LocalDate.now();
        importKurzyCzToFile(exchange, from, to);
    }

    public void importKurzyCzToFile(Exchange exchange, LocalDate from, LocalDate to)
            throws IOException, InterruptedException {

        List<KurzyCzPrice> prices = new ArrayList<>();

        ObjectMapper objectMapper = new ObjectMapper();
        String fileName = "KurzyCZ_" + exchange.getAbbreviation() + "_"
                + from.format(DateTimeFormatter.ISO_DATE) + "_" + to.format(DateTimeFormatter.ISO_DATE) + ".json";
        File file = new File(fileName);
        if (!file.exists()) {
            List<LocalDate> dates = from.datesUntil(to.plusDays(1)).collect(Collectors.toList());
            for (LocalDate date : dates) {
                prices.add(kurzyCzClient.importPrice(exchange, date));
                Thread.sleep(new Random().nextInt(10) + 50l);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, prices);
        }
    }

    public void importAssetFromKurzyCzFile() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        File file = new File("KurzyCZ_BCPP_2001-10-01_2020-02-06.json");
        if (file.exists()) {
            List<KurzyCzPrice> prices = objectMapper.readValue(file, new TypeReference<List<KurzyCzPrice>>() {
            });
            prices.stream()
                    .map(map -> map.getListOfPrices())
                    .flatMap(Collection::stream)
                    .map(map -> {
                        String ticker = null;
                        if (!map.getBic().equals("-")) {
                            ticker = map.getBic();
                        }

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
                    .forEach(el -> {
                        assetRepository.save(el);
                    });

        }
    }

    public void importPriceFromKurzyCzFile() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        File file = new File("KurzyCZ_BCPP_2001-10-01_2020-02-06.json");
        if (file.exists()) {
            List<KurzyCzPrice> prices = objectMapper.readValue(file, new TypeReference<List<KurzyCzPrice>>() {
            });
            Asset priceAsset = assetRepository.findByTicker("CZK").orElseThrow();
            Exchange exchange = exchangeRepository.findByAbbreviation(ExchangeAbbrEnum.BCPP).orElseThrow();
            prices.stream()
                    .map(map -> map.getListOfPrices())
                    .flatMap(Collection::stream)
                    .map(map -> {
                        Asset asset;
                        Optional<Asset> assetOpt = assetRepository.findByIsin(map.getIsin());
                        if (assetOpt.isEmpty()) {
                            log.debug("ISIN not found. -> {}", map);
                            return null;
                        } else {
                            asset = assetOpt.get();
                        }
                        return Price.builder()
                                .date(LocalDate.parse(map.getTradingDay()))
                                .asset(asset)
                                .exchange(exchange)
                                .priceAsset(priceAsset)
                                .openingPrice(new BigDecimal(map.getPriceOpen()))
                                .closingPrice(new BigDecimal(map.getPriceClose()))
                                .priceChange(new BigDecimal(map.getPriceChange()))
                                .minPrice(new BigDecimal(map.getPriceMinDay()))
                                .maxPrice(new BigDecimal(map.getPriceMaxDay()))
                                .volume(new BigDecimal(map.getTradeShares()))
                                .turnover(new BigDecimal(map.getTradeVolume()))
                                .build();
                    })
                    .distinct()
                    .forEach(el -> {
                        priceRepository.save(el);
                    });

        }
    }

}
