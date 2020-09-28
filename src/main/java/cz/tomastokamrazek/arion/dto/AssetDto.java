package cz.tomastokamrazek.arion.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class AssetDto {
    
    String name;
    String ticker;
    String isin;
    String type;
    String nominalPriceAsset;
    BigDecimal nominalPrice;
    
}
