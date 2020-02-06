package cz.jaktoviditoka.investmentportfolio.domain;

import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class PortfolioAsset {

    Asset asset;
    BigDecimal amount;
    BigDecimal value;
    
}
