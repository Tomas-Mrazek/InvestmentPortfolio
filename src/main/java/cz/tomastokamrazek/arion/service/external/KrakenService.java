package cz.tomastokamrazek.arion.service.external;

import java.util.Map;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tomastokamrazek.arion.datapoint.kraken.KrakenClient;
import cz.tomastokamrazek.arion.datapoint.kraken.dto.KrakenAssetInfo;
import cz.tomastokamrazek.arion.datapoint.kraken.dto.KrakenLedger;
import cz.tomastokamrazek.arion.datapoint.kraken.dto.KrakenResponse;
import cz.tomastokamrazek.arion.datapoint.kraken.dto.KrakenResponseAssetInfo;

@Service
public class KrakenService {

	private static final Map<String, String> assetMapping = Map.of(
		
	);
	
	@Autowired
	KrakenClient client;
	
	@Autowired
	ModelMapper mapper;
	
	public void importAssets() {
		KrakenResponseAssetInfo response = client.getAssetInfo().getBody();
		Map<String, KrakenAssetInfo> assets = response.getResult();
	}
	
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
