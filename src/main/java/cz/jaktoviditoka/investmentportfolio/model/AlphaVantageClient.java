package cz.jaktoviditoka.investmentportfolio.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.AssetPriceHistory;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.repository.AssetPriceHistoryRepository;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AlphaVantageClient {

    @Autowired
    AssetPriceHistoryRepository assetPriceHistoryRepository;
    
    @Autowired
    RestTemplate restTemplate;

    String apiKey = "1NGB6KWLNMXRI43S";

    String urlTemplate = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=${assetTicker}&outputsize=full&apikey=${apiKey}";

    public void getAssetHistoricPrice(Asset asset, Exchange exchange, LocalDate minDate) throws IOException {        
        LocalDate maxDate = LocalDate.now();
        List<LocalDate> existingDates = assetPriceHistoryRepository.findByAssetAndExchange(asset, exchange).stream()
                .map(mapper -> mapper.getDate())
                .collect(Collectors.toList());
        List<LocalDate> processingDates = minDate.datesUntil(maxDate)
                .collect(Collectors.toList());
        processingDates.removeAll(existingDates);
        
        if (processingDates.isEmpty()) {
            return;
        }
        
        URI uri = UriComponentsBuilder.fromHttpUrl("https://www.alphavantage.co")
                .path("/query")
                .queryParam("function", "TIME_SERIES_DAILY")
                .queryParam("symbol", asset.getTicker())
                .queryParam("outputsize", "full")
                .queryParam("apikey", apiKey)
                .build()
                .toUri();
        
        log.debug("url: {}", uri);
        
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        
        ObjectMapper mapper = new ObjectMapper();

       JsonNode root = mapper.readTree(response.getBody());
       root.get("Time Series (Daily)").fields().forEachRemaining(el -> {
           LocalDate date = LocalDate.parse(el.getKey(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
           if (processingDates.contains(date)) {
               log.debug("Scraping date: {}", date);
               
               processingDates.remove(date);
               
               AssetPriceHistory assetPriceHistory = new AssetPriceHistory();
               assetPriceHistory.setDate(LocalDate.parse(el.getKey(), DateTimeFormatter.ofPattern("yyyy-MM-dd")));
               assetPriceHistory.setAsset(asset);
               assetPriceHistory.setExchange(exchange);
               assetPriceHistory.setClosingPrice(new BigDecimal(el.getValue().get("4. close").asText()));
               
               assetPriceHistoryRepository.save(assetPriceHistory);
               
               log.debug("Scraping price history: {}", assetPriceHistory);
           }
       });

    }

}
