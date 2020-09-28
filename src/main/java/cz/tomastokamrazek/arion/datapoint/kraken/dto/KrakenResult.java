package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class KrakenResult {

	@JsonProperty("ledger")
	public Map<String, KrakenLedger> ledger;
	
	@JsonProperty("trades")
	public Map<String, KrakenTrade> trades;
	
	@JsonProperty("count")
	public Integer count;
	
}
