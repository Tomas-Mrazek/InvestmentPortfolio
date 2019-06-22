package cz.jaktoviditoka.investmentscraper.domain;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FioEbrokerTransaction {

    LocalDateTime timestamp;
    String type;
    String asset;
    BigDecimal price;
    BigDecimal amount;
    String currency;
    BigDecimal totalAmount;
    String totalAmountAsset;
    BigDecimal feeAmount;
    String feeAsset;
    String comment;
    
}
