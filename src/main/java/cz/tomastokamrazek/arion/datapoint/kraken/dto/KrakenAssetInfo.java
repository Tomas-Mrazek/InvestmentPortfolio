package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KrakenAssetInfo {

	@JsonProperty("aclass")
	String aclass;
	
	@JsonProperty("altname")
	String altName;
	
	@JsonProperty("decimals")
	Integer decimals;
	
	@JsonProperty("display_decimals")
	Integer displayDecimals;
	
}
