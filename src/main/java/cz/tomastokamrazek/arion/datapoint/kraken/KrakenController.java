package cz.tomastokamrazek.arion.datapoint.kraken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tomastokamrazek.arion.datapoint.kraken.dto.KrakenResponse;

@RestController
@RequestMapping("/external/kraken")
public class KrakenController {

	@Autowired
	KrakenClient client;
    
    @GetMapping("/account-balance")
    public ResponseEntity<String> getAccountBalance() {
        return client.getAccountBalance();
    }
    
    @GetMapping("/trade-balance")
    public ResponseEntity<String> getTradeBalance() {
        return client.getTradeBalance();
    }
    
    @GetMapping("/trades-history")
    public ResponseEntity<KrakenResponse> getTradesHiostry() {
        return client.getTradesHiostry();
    } 
    
    @GetMapping("/ledgers-info")
    public ResponseEntity<KrakenResponse> getLedgersInfo() {
        return client.getLedgersInfo();
    }    
	
}
