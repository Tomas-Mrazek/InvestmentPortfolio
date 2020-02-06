package cz.jaktoviditoka.investmentportfolio.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class PortfolioAssetPerDay {

    LocalDate date;
    List<PortfolioAsset> assets;
    
}
