package cz.jaktoviditoka.investmentportfolio.dto.transaction;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionTradeRequest {

    @NotNull
    LocalDateTime timestamp;
    
    @NotNull
    String userId;
    
    @NotNull
    TransactionType type = TransactionType.TRADE;
   
    @NotNull
    TransactionPartDto buy;
    
    @NotNull
    TransactionPartDto sell;
    
}
