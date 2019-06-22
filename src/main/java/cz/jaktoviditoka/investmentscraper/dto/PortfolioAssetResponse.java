package cz.jaktoviditoka.investmentscraper.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.constraints.NotNull;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PortfolioAssetResponse {

    @NotNull
    Long id;
    
    @NotNull
    Long userId;
    
    @NotNull
    LocalDate date;
    
    @NotNull
    Long assetId;
    
    @NotNull
    BigDecimal amount;
    
    Long exchangeId;
    
    @NotNull
    Long locationId;
    
    @NotNull
    Long transactionId;
    
}
