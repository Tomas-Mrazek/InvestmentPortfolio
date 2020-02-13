package cz.jaktoviditoka.arion.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class PortfolioDayValueDto {

    LocalDate date;
    BigDecimal value;
    
}
