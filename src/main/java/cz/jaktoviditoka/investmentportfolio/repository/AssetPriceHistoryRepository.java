package cz.jaktoviditoka.investmentportfolio.repository;

import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.AssetPriceHistory;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetPriceHistoryRepository extends JpaRepository<AssetPriceHistory, Long> {

    List<AssetPriceHistory> findByAssetAndExchange(Asset asset, Exchange exchange);
    
}
