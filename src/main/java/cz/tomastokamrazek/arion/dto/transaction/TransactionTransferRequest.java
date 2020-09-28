package cz.tomastokamrazek.arion.dto.transaction;

import cz.tomastokamrazek.arion.domain.TransactionType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionTransferRequest {

    @NotNull
    LocalDateTime timestamp;
    
    @NotNull
    String userId;
    
    @NotNull
    TransactionType type = TransactionType.TRANSFER;
   
    @NotNull
    TransactionPartDto from;
    
    @NotNull
    TransactionPartDto to;
    
}
