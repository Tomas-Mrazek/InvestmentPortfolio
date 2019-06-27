package cz.jaktoviditoka.investmentportfolio.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PortfolioAssetGroupedDto {

    LocalDate date;
    Long assetId;
    BigDecimal amount;
    Long exchangeId;
    Long locationId;

}
