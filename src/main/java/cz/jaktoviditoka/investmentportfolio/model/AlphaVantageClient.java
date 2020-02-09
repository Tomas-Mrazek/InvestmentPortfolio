package cz.jaktoviditoka.investmentportfolio.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.jaktoviditoka.investmentportfolio.domain.AssetType;
import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.Price;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.PriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class AlphaVantageClient {

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    PriceRepository assetPriceRepository;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    private static final String API_KEY = "1NGB6KWLNMXRI43S";
    private static final String PRICE_ASSET = "USD";

    public void getAssetHistoricPrice(String ticker) throws IOException {
        log.debug("Importing – {}", ticker);

        Asset priceAsset = assetRepository.findByTicker(PRICE_ASSET)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        URI uri = UriComponentsBuilder.fromHttpUrl("https://www.alphavantage.co")
                .path("/query")
                .queryParam("function", "TIME_SERIES_DAILY")
                .queryParam("symbol", ticker)
                .queryParam("outputsize", "full")
                .queryParam("apikey", API_KEY)
                .build()
                .toUri();
        log.trace("url: {}", uri);

        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

        List<Price> prices = new ArrayList<>();

        JsonNode root = objectMapper.readTree(response.getBody());

        if (Objects.nonNull(root.get("Error Message"))) {
            log.warn("{}", root.get("Error Message").asText());
            return;
        }

        String symbol = root.get("Meta Data").get("2. Symbol").asText();
        Optional<Asset> assetOpt = assetRepository.findByTicker(symbol);
        Asset asset;
        if (assetOpt.isEmpty()) {
            asset = Asset.builder()
                    .name(symbol)
                    .ticker(symbol)
                    .type(AssetType.STOCK)
                    .build();
            assetRepository.save(asset);
        } else {
            asset = assetOpt.get();
        }

        root.get("Time Series (Daily)").fields().forEachRemaining(el -> {
            Price price = Price.builder()
                    .date(LocalDate.parse(el.getKey(), DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .asset(asset)
                    .openingPrice(new BigDecimal(el.getValue().get("1. open").asText()))
                    .lowPrice(new BigDecimal(el.getValue().get("3. low").asText()))
                    .highPrice(new BigDecimal(el.getValue().get("2. high").asText()))
                    .closingPrice(new BigDecimal(el.getValue().get("4. close").asText()))
                    .priceAsset(priceAsset)
                    .build();
            prices.add(price);
        });

        assetPriceRepository.saveAll(prices);
    }

}
