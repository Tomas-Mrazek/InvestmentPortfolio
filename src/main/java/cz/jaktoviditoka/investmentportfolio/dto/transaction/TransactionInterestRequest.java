package cz.jaktoviditoka.investmentportfolio.dto.transaction;

import cz.jaktoviditoka.investmentportfolio.domain.TransactionType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionInterestRequest {

    @NotNull
    LocalDateTime timestamp;
    
    @NotNull
    String userId;
    
    @NotNull
    TransactionType type = TransactionType.INTEREST;
    
    @NotNull
    TransactionPartDto interest;
    
}
