package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KrakenResponse {

	@JsonProperty("result")
	KrakenResult result;
	
}
