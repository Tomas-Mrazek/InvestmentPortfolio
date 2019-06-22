package cz.jaktoviditoka.investmentscraper.repository;

import cz.jaktoviditoka.investmentscraper.entity.Asset;
import cz.jaktoviditoka.investmentscraper.entity.AssetPriceHistory;
import cz.jaktoviditoka.investmentscraper.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetPriceHistoryRepository extends JpaRepository<AssetPriceHistory, Long> {

    List<AssetPriceHistory> findByAssetAndExchange(Asset asset, Exchange exchange);
    
}
