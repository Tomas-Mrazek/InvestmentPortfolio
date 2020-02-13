package cz.jaktoviditoka.arion.dto;

import cz.jaktoviditoka.arion.domain.AssetType;
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
