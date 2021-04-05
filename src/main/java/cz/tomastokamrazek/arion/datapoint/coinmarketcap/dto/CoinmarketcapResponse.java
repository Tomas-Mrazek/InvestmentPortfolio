package cz.tomastokamrazek.arion.datapoint.coinmarketcap.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoinmarketcapResponse {
	
	@JsonProperty("status")
	CoinmarketcapStatus status;
	
	@JsonProperty("data")
	List<CoinmarketcapCryptocurrency> data;	
	
}	
