package cz.tomastokamrazek.arion.datapoint.coinmarketcap.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoinmarketcapCryptocurrency {

	@JsonProperty("id")
	Long id;
	
	@JsonProperty("name")
	String name;
	
	@JsonProperty("symbol")
	String symbol;
	
	@JsonProperty("slug")
	String slug;
	
	@JsonProperty("is_active")
	Boolean isActive;
	
	@JsonProperty("rank")
	Boolean rank;
	
	@JsonProperty("first_historical_data")
	Instant firstHistoricalData;
	
	@JsonProperty("last_historical_data")
	Instant lastHistoricalData;
	
	@JsonProperty("platform")
	CoinmarketcapPlatform platform;
	
}
