package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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
