package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KrakenResponse {

	@JsonProperty("result")
	KrakenResult result;
	
}
