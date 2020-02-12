package cz.jaktoviditoka.investmentportfolio.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class PortfolioHistoryDayDto {

    String date;
    String assetTicker;
    BigDecimal amount;
    BigDecimal value;

}
