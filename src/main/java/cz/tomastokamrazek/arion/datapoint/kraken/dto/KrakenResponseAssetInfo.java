package cz.tomastokamrazek.arion.datapoint.kraken.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KrakenResponseAssetInfo {

	@JsonProperty("result")
	public Map<String, KrakenAssetInfo> result;
	
}
