package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KrakenTrade {

	@JsonProperty("ordertxid")
	String orderTxId;
	
	@JsonProperty("postxid")
	String posTxId;
	
	@JsonProperty("pair")
	String pair;
	
	@JsonProperty("time")
	Double time;
	
	@JsonProperty("type")
	KrakenTradeType type;
	
	@JsonProperty("ordertype")
	String orderType;
	
	@JsonProperty("price")
	BigDecimal price;
	
	@JsonProperty("cost")
	BigDecimal cost;

	@JsonProperty("fee")
	BigDecimal fee;
	
	@JsonProperty("vol")
	BigDecimal vol;
	
	@JsonProperty("margin")
	BigDecimal margin;
	
	@JsonProperty("misc")
	String misc;
	
}
