package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum KrakenTradeType {

	@JsonProperty("buy")
	BUY("buy"),
	
	@JsonProperty("sell")
	SELL("sell");
	
	String value;
	
	KrakenTradeType(String value) {
		this.value = value;
	}
	
}
