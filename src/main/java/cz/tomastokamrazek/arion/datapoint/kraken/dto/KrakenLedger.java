package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KrakenLedger {

	@JsonProperty("refid")
	String refId;
	
	@JsonProperty("time")
	Double time;
	
	@JsonProperty("type")
	KrakenLedgerType type;
	
	@JsonProperty("subtype")
	String subtype;
	
	@JsonProperty("aclass")
	String aclass;
	
	@JsonProperty("asset")
	String asset;
	
	@JsonProperty("amount")
	BigDecimal amount;
	
	@JsonProperty("fee")
	BigDecimal fee;
	
	@JsonProperty("balance")
	BigDecimal balance;
	
}
