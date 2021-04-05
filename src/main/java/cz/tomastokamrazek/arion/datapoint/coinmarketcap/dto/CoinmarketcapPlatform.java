package cz.tomastokamrazek.arion.datapoint.coinmarketcap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoinmarketcapPlatform {
	
	@JsonProperty("id")
	Long id;
	
	@JsonProperty("name")
	String name;
	
	@JsonProperty("symbol")
	String symbol;
	
	@JsonProperty("slug")
	String slug;
	
	@JsonProperty("token_address")
	String tokenAddress;

}
