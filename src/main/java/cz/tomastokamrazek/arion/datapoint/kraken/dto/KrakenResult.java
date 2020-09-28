package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KrakenResult {

	@JsonInclude(Include.NON_NULL)
	@JsonProperty("ledger")
	public Map<String, KrakenLedger> ledger;
	
	@JsonInclude(Include.NON_NULL)
	@JsonProperty("trades")
	public Map<String, KrakenTrade> trades;
	
	@JsonProperty("count")
	public Integer count;
	
}
