package cz.tomastokamrazek.arion.datapoint.coinmarketcap;

import java.net.URI;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import cz.tomastokamrazek.arion.config.SecretsConfig;
import cz.tomastokamrazek.arion.datapoint.coinmarketcap.dto.CoinmarketcapResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CoinmarketcapClient {
	
	private static final String COINMARKETCAP_API_HOST = "https://pro-api.coinmarketcap.com";
	
	@Autowired
	SecretsConfig secrets;
	
    @Autowired
    RestTemplate restTemplate;
    
    @Cacheable("cryptocurrency_map")
    public CoinmarketcapResponse getCryptocurrencyMap() {
        URI uri = UriComponentsBuilder.fromHttpUrl(COINMARKETCAP_API_HOST)
                .path("/v1/cryptocurrency")
                .path("/map")
                .queryParam("listing_status", "active,inactive")
                .build()
                .toUri();
    	
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("X-CMC_PRO_API_KEY", secrets.getCoinmarketcapApiKey());
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
        
        ResponseEntity<CoinmarketcapResponse> response = restTemplate.exchange(uri, HttpMethod.GET, request, CoinmarketcapResponse.class);
        
        return response.getBody();
    }
	
}
