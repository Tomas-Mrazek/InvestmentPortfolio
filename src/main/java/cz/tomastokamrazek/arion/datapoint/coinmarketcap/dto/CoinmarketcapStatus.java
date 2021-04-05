package cz.tomastokamrazek.arion.datapoint.coinmarketcap.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CoinmarketcapStatus {

	@JsonProperty("timestamp")
	Instant timestamp;
	
	@JsonProperty("error_code")
	Integer errorCode;
	
	@JsonProperty("error_message")
	String errorMessage;
	
	@JsonProperty("elapsed")
	Integer elapsed;
	
	@JsonProperty("credit_count")
	Integer creditCount;
	
	@JsonProperty("notice")
	String notice;
	
}
