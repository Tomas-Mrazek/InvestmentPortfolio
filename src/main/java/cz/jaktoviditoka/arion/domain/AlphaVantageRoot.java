package cz.jaktoviditoka.arion.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AlphaVantageRoot {

    @JsonProperty("Meta Data")
    AlphaVantageMetadata metadata;
    
    @JsonProperty("Time Series (Daily)")
    List<AlphaVantagePriceHistory> priceHistory;
    
}
