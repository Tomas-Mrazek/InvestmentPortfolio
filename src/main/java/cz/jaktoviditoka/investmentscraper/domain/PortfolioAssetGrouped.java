package cz.jaktoviditoka.investmentscraper.domain;

import cz.jaktoviditoka.investmentscraper.entity.Asset;
import cz.jaktoviditoka.investmentscraper.entity.Exchange;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PortfolioAssetGrouped {

    Asset asset;
    BigDecimal amount;
    Exchange exchange;

}
