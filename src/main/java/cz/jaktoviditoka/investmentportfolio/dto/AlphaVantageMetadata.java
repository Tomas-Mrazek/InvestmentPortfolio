package cz.jaktoviditoka.investmentportfolio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AlphaVantageMetadata {

    @JsonProperty("1. Information")
    String information;
    
    @JsonProperty("2. Symbol")
    String symbol;
    
    @JsonProperty("3. Last Refreshed")
    String lastRefreshed;
    
    @JsonProperty("4. Output Size")
    String outputSize;
    
    @JsonProperty("5. Time Zone")
    String timeZone;
    
}
