package cz.tomastokamrazek.arion.service.external;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cz.tomastokamrazek.arion.datapoint.coinmarketcap.CoinmarketcapClient;
import cz.tomastokamrazek.arion.datapoint.coinmarketcap.dto.CoinmarketcapCryptocurrency;
import cz.tomastokamrazek.arion.datapoint.coinmarketcap.dto.CoinmarketcapResponse;
import cz.tomastokamrazek.arion.domain.AssetType;
import cz.tomastokamrazek.arion.entity.Asset;
import cz.tomastokamrazek.arion.repository.AssetRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CoinmarketcapService {

	@Autowired
	CoinmarketcapClient client;

	@Autowired
	AssetRepository assetRepository;

	public void importAssets() {
		List<Asset> existingAssets = assetRepository.findAll();

		CoinmarketcapResponse response = client.getCryptocurrencyMap();
		List<CoinmarketcapCryptocurrency> assets = response.getData();

		List<Asset> newAssets = assets.stream()			
			.map(el -> {
				return Asset.builder()
						.name(el.getName())
						.ticker(el.getSymbol())
						.type(AssetType.CRYPTOCURRENCY)
						.build();
			})
			.filter(el -> BooleanUtils.isFalse(existingAssets.contains(el)))
			.collect(Collectors.toList());
		
		log.debug("Saving {} cryptocurrencies to database...", newAssets.size());
		assetRepository.saveAll(newAssets);
		log.debug("Done...");
	}

}
