package cz.jaktoviditoka.investmentportfolio.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionPartDto {

    @NotNull
    Long assetId;
    
    @NotNull
    BigDecimal amount;
    
    Long feeAssetId;
    
    BigDecimal feeAmount;
    
    @NotNull
    Long locationId;
    
    Long exchangeId;
    
}
