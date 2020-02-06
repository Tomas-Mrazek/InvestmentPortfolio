package cz.jaktoviditoka.investmentportfolio.repository;

import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {

    List<Price> findByAssetAndExchange(Asset asset, Exchange exchange);
    
    List<Price> findByAssetAndPriceAssetAndExchange(Asset asset, Asset priceAsset, Exchange exchange);

    @Query("SELECT p FROM Price p WHERE asset = :asset AND (priceAsset = :priceAsset OR priceAsset IS NULL) AND exchange = :exchange")
    List<Price> findByAssetAndPriceAssetAndPriceAssetIsNullAndExchange(
            @Param("asset") Asset asset,
            @Param("priceAsset") Asset priceAsset,
            @Param("exchange") Exchange exchange);

}
