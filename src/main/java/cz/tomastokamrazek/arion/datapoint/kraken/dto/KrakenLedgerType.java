package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum KrakenLedgerType {

	@JsonProperty("withdrawal")
	WITHDRAWAL("withdrawal"),
	
	@JsonProperty("deposit")
	DEPOSIT("deposit"),
	
	@JsonProperty("trade")
	TRADE("trade"),
	
	@JsonProperty("margin")
	MARGIN("margin");
	
	String value;
	
	KrakenLedgerType(String value) {
		this.value = value;
	}
	
}
