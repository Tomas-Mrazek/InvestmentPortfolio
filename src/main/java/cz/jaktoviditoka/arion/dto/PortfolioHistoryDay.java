package cz.jaktoviditoka.arion.dto;

import cz.jaktoviditoka.arion.entity.Asset;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class PortfolioHistoryDay {

    LocalDate date;
    Asset asset;
    BigDecimal amount;
    BigDecimal value;

}
