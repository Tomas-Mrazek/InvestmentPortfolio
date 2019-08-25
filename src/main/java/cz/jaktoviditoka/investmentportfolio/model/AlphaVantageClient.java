package cz.jaktoviditoka.investmentportfolio.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.AssetPrice;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.repository.AssetPriceRepository;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AlphaVantageClient {

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetPriceRepository assetPriceRepository;

    @Autowired
    RestTemplate restTemplate;

    private static final String API_KEY = "1NGB6KWLNMXRI43S";
    private static final String PRICE_ASSET = "USD";

    public void getAssetHistoricPrice(Asset asset, Exchange exchange, LocalDate scrapeDate) throws IOException {
        log.trace("Scraping...");

        Asset priceAsset = assetRepository.findByName(PRICE_ASSET)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        List<LocalDate> existingDates = assetPriceRepository.findByAssetAndExchange(asset, exchange).stream()
                .map(mapper -> mapper.getDate())
                .collect(Collectors.toList());

        List<LocalDate> missingDates = scrapeDate.datesUntil(LocalDate.now())
                .filter(el -> BooleanUtils.isNotTrue(existingDates.contains(el)))
                .collect(Collectors.toList());

        URI uri = UriComponentsBuilder.fromHttpUrl("https://www.alphavantage.co")
                .path("/query")
                .queryParam("function", "TIME_SERIES_DAILY")
                .queryParam("symbol", asset.getTicker())
                .queryParam("outputsize", "full")
                .queryParam("apikey", API_KEY)
                .build()
                .toUri();
        log.trace("url: {}", uri);

        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

        ObjectMapper mapper = new ObjectMapper();

        JsonNode root = mapper.readTree(response.getBody());
        root.get("Time Series (Daily)").fields().forEachRemaining(el -> {
            LocalDate date = LocalDate.parse(el.getKey(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if (missingDates.contains(date)) {
                log.debug("Scraping date: {}", date);

                AssetPrice assetPrice = AssetPrice.builder()
                        .date(LocalDate.parse(el.getKey(), DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .asset(asset)
                        .price(new BigDecimal(el.getValue().get("4. close").asText()))
                        .priceAsset(priceAsset)
                        .exchange(exchange)
                        .build();
                assetPriceRepository.save(assetPrice);
            }
        });

        log.trace("Scraping finished...");
    }

}
