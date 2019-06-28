package cz.jaktoviditoka.investmentportfolio.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PortfolioAssetPerDayValueDto {

    LocalDate date;
    BigDecimal value;
    BigDecimal change;
    BigDecimal percentualChange;
    List<PortfolioAssetGroupedDto> assets;
    
    
}
