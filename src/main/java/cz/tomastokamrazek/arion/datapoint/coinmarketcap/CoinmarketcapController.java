package cz.tomastokamrazek.arion.datapoint.coinmarketcap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cz.tomastokamrazek.arion.datapoint.coinmarketcap.dto.CoinmarketcapResponse;

@RestController
@RequestMapping("/external/coinmarketcap")
public class CoinmarketcapController {

	@Autowired
	CoinmarketcapClient client;
	
    @GetMapping("/cryptocurrency-map")
    public CoinmarketcapResponse getCryptocurrencyMap() {
        return client.getCryptocurrencyMap();
    }
	
}
