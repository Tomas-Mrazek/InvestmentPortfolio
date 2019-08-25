package cz.jaktoviditoka.investmentportfolio.dto.transaction;

import cz.jaktoviditoka.investmentportfolio.domain.TransactionType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionResponse {

    @NotNull
    Long id;
    
    @NotNull
    LocalDateTime timestamp;
    
    @NotNull
    Long userId;
    
    @NotNull
    TransactionType type;
   
    @NotNull
    TransactionPartDto in;
    
    @NotNull
    TransactionPartDto out; 
    
    @NotNull
    Boolean imported;
    
}
