package cz.tomastokamrazek.arion.service.external;

import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tomastokamrazek.arion.datapoint.kraken.KrakenClient;
import cz.tomastokamrazek.arion.datapoint.kraken.dto.KrakenLedger;
import cz.tomastokamrazek.arion.datapoint.kraken.dto.KrakenResponse;

@Service
public class KrakenService {

	@Autowired
	KrakenClient client;
	
	@Autowired
	ModelMapper mapper;
	
	public void importLedger() {
		KrakenResponse response = client.getLedgersInfo().getBody();
		Map<String, KrakenLedger> ledger = response.getResult().getLedger();
		ledger.entrySet().stream()
			.map(el -> el.getValue())
			.map(el -> {
				
				
				return el;
			})
			.close();
	}
	
}
