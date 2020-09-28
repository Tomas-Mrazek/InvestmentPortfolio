package cz.tomastokamrazek.arion.repository;

import cz.tomastokamrazek.arion.entity.Asset;
import cz.tomastokamrazek.arion.entity.Exchange;
import cz.tomastokamrazek.arion.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {

    List<Price> findByAsset(Asset asset);
    
    List<Price> findByDate(LocalDate date);
    
    List<Price> findByAssetAndDate(Asset asset, LocalDate date);
    
    List<Price> findByAssetAndExchange(Asset asset, Exchange exchange);
    
    List<Price> findByAssetAndPriceAssetAndExchange(Asset asset, Asset priceAsset, Exchange exchange);

    @Query("SELECT p FROM Price p WHERE asset = :asset AND (priceAsset = :priceAsset OR priceAsset IS NULL) AND exchange = :exchange")
    List<Price> findByAssetAndPriceAssetAndPriceAssetIsNullAndExchange(
            @Param("asset") Asset asset,
            @Param("priceAsset") Asset priceAsset,
            @Param("exchange") Exchange exchange);

}
