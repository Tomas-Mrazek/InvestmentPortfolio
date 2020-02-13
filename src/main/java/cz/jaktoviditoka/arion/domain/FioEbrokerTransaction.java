package cz.jaktoviditoka.arion.domain;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FioEbrokerTransaction {

    ZonedDateTime timestamp;
    String type;
    String asset;
    BigDecimal amount;
    BigDecimal price;
    String currency;
    BigDecimal totalAmount;
    String totalAmountCurrency;
    BigDecimal feeAmount;
    String feeCurrency;
    String comment;
    
}
