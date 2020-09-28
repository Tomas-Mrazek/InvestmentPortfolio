package cz.tomastokamrazek.arion.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import cz.tomastokamrazek.arion.datapoint.finnhub.FinnhubClient;
import cz.tomastokamrazek.arion.datapoint.finnhub.FinnhubStockProfileResponse;
import cz.tomastokamrazek.arion.security.HasAdminAuthority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@HasAdminAuthority
@RestController
@RequestMapping("/datapoint")
public class DatapointController {

    @Autowired
    FinnhubClient finnhubClient;
    
    @GetMapping("/finnhub/stock/profile")
    public FinnhubStockProfileResponse getStockProfile(@RequestParam String ticker) throws JsonProcessingException {
        return finnhubClient.getStockProfile(ticker).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/finnhub/stock/exchange")
    public String getExchanges() throws JsonProcessingException {
        return finnhubClient.getExchanges();
    }
    
}
