package cz.tomastokamrazek.arion.datapoint.kraken;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.hash.Hashing;
import com.google.common.primitives.Bytes;

import cz.tomastokamrazek.arion.datapoint.kraken.dto.KrakenResponse;
import lombok.extern.slf4j.Slf4j;

@Component
public class KrakenClient {

	private static final String API_KEY = "kraken.api.key";
	private static final String PRIVATE_KEY = "kraken.private.key";	
	
	private static final String KRAKEN_API_HOST = "https://api.kraken.com";
	private static final String KRAKEN_API_PATH = "/0/private";
	
	@Autowired
	Environment enviornment;
	
    @Autowired
    RestTemplate restTemplate;
	
	public ResponseEntity<String> getAccountBalance() {
        URI uri = UriComponentsBuilder.fromHttpUrl(KRAKEN_API_HOST)
                .path(KRAKEN_API_PATH)
                .path("/Balance")
                .build()
                .toUri();
        
        HttpEntity<MultiValueMap<String, String>> request = prepareRequest(uri);
        
        return restTemplate.exchange(uri, HttpMethod.POST, request, String.class);
	}
	
	public ResponseEntity<String> getTradeBalance() {
        URI uri = UriComponentsBuilder.fromHttpUrl(KRAKEN_API_HOST)
                .path(KRAKEN_API_PATH)
                .path("/TradeBalance")
                .build()
                .toUri();
        
        HttpEntity<MultiValueMap<String, String>> request = prepareRequest(uri);
        
        return restTemplate.exchange(uri, HttpMethod.POST, request, String.class);
	}
	
	public ResponseEntity<KrakenResponse> getTradesHiostry() {
        URI uri = UriComponentsBuilder.fromHttpUrl(KRAKEN_API_HOST)
                .path(KRAKEN_API_PATH)
                .path("/TradesHistory")
                .build()
                .toUri();
        
        HttpEntity<MultiValueMap<String, String>> request = prepareRequest(uri);
        
        return restTemplate.exchange(uri, HttpMethod.POST, request, KrakenResponse.class);
	}
	
	public ResponseEntity<KrakenResponse> getLedgersInfo() {
        URI uri = UriComponentsBuilder.fromHttpUrl(KRAKEN_API_HOST)
                .path(KRAKEN_API_PATH)
                .path("/Ledgers")
                .build()
                .toUri();
        
        HttpEntity<MultiValueMap<String, String>> request = prepareRequest(uri);
        
        return restTemplate.exchange(uri, HttpMethod.POST, request, KrakenResponse.class);
	}
	
	private HttpEntity<MultiValueMap<String, String>> prepareRequest(URI uri) {
        byte[] apiSecret = Base64.getDecoder().decode(enviornment.getProperty(PRIVATE_KEY));
        String apiPath = uri.getPath();
        String apiNonce = Long.toString(generateNonce());
        String apiPostData = new StringBuilder()
        		.append("nonce=")
        		.append(apiNonce)
        		.toString();

        byte[] sha256 = Hashing.sha256()
        	.hashBytes(Bytes.concat(apiNonce.getBytes(), apiPostData.getBytes()))
        	.asBytes();
        
        byte[] hmacSha512 = Hashing.hmacSha512(apiSecret)
        	.hashBytes(Bytes.concat(apiPath.getBytes(), sha256))
        	.asBytes();
        
        String apiSign = Base64.getEncoder().encodeToString(hmacSha512);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add("API-Key", enviornment.getProperty(API_KEY));
        headers.add("API-Sign", apiSign);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("nonce", apiNonce);

        return new HttpEntity<>(body, headers);
	}
	
	private Long generateNonce() {
		return Instant.now().toEpochMilli();
	}
	
}
