package cz.jaktoviditoka.investmentportfolio.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class PriceDto {

    Long id;
    LocalDate date;
    String exchange;
    String priceAssetTicker;
    BigDecimal openingPrice;
    BigDecimal minPrice;
    BigDecimal maxPrice;
    BigDecimal closingPrice;
    BigDecimal volume;
    BigDecimal turnover;
    
}
