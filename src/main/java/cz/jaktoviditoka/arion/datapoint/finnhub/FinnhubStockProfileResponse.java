package cz.jaktoviditoka.arion.datapoint.finnhub;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class FinnhubStockProfileResponse {

    String name;
    String isin;
    String exchange;
    
}
