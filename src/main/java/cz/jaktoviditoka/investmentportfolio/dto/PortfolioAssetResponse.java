package cz.jaktoviditoka.investmentportfolio.dto;

import cz.jaktoviditoka.investmentportfolio.domain.AssetType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class PortfolioAssetResponse {

    Long assetId;
    String assetName;
    String assetTicker;
    AssetType assetType;
    String exchange;
    String location;
    BigDecimal amount;
    BigDecimal value;
    
}
