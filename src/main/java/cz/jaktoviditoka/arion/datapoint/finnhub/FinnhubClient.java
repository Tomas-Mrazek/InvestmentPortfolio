package cz.jaktoviditoka.arion.datapoint.finnhub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@Slf4j
@Component
public class FinnhubClient {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    private static final String API_KEY = "bp295tnrh5r9d7scfga0";

    public Optional<FinnhubStockProfileResponse> getStockProfile(String ticker) throws JsonProcessingException {
        URI uri = UriComponentsBuilder.fromHttpUrl("https://finnhub.io")
                .path("/api/v1")
                .path("/stock/profile")
                .queryParam("symbol", ticker)
                .queryParam("token", API_KEY)
                .build()
                .toUri();
        
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        
        JsonNode root = objectMapper.readTree(response.getBody());
        
        if (root.size() == 0) {
            return Optional.empty();
        }

        return Optional.of(FinnhubStockProfileResponse.builder()
                .name(root.path("name").asText())
                .isin(root.path("isin").asText())
                .exchange(root.path("exchange").asText())
                .build());
    }
    
    public String getExchanges() throws JsonProcessingException {
        URI uri = UriComponentsBuilder.fromHttpUrl("https://finnhub.io")
                .path("/api/v1")
                .path("/stock/exchange")
                .queryParam("token", API_KEY)
                .build()
                .toUri();
        
        ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
        
        //JsonNode root = objectMapper.readTree(response.getBody());
        
        return response.getBody();
    }

}
