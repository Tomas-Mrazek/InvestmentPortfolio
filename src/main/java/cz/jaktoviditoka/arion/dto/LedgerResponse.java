package cz.jaktoviditoka.arion.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class LedgerResponse {

    Long id;
    ZonedDateTime timestamp;
    BigDecimal amount;
    String assetName;
    String exchangeName;
    String location;
    
}
