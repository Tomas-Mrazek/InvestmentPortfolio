package cz.jaktoviditoka.investmentportfolio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AlphaVantagePriceHistory {

    @JsonProperty("1. open")
    BigDecimal open;
    
    @JsonProperty("2. high")
    BigDecimal high;
    
    @JsonProperty("3. low")
    BigDecimal low;
    
    @JsonProperty("4. close")
    BigDecimal close;
    
    @JsonProperty("5. volume")
    BigDecimal volume;
    
}
