package cz.tomastokamrazek.arion.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class FioForexPrice {

    String date;
    String asset;
    String country;
    String amount;
    String buyPrice;
    String sellPrice;
    String cnbPrice;
    
}
