package cz.jaktoviditoka.arion.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class KurzyCzPriceDay {
    
    String uri;
    String slug;
    String type;
    String isin;
    String bic;
    String name;
    String tradingDay;
    String priceClose;
    String priceChange;
    String priceOpen;
    String priceMinDay;
    String priceMaxDay;
    String priceMinYear;
    String priceMaxYear;
    String tradeShares;
    String tradeVolume;

}
