package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum KrakenLedgerType {

	@JsonProperty("withdrawal")
	WITHDRAWAL("withdrawal"),
	
	@JsonProperty("deposit")
	DEPOSIT("deposit"),
	
	@JsonProperty("trade")
	TRADE("trade");
	
	String value;
	
	KrakenLedgerType(String value) {
		this.value = value;
	}
	
}
