package cz.jaktoviditoka.investmentscraper.repository;

import cz.jaktoviditoka.investmentscraper.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, Long> {
    
    List<PortfolioAsset> findByUserId(Long userId);

    List<PortfolioAsset> findByUserAndAsset(User user, Asset asset);

    List<PortfolioAsset> findByUserAndAssetAndLocation(User user, Asset asset, Location location);
    
    @Query("SELECT min(date) FROM PortfolioAsset WHERE asset = :asset AND exchange = :exchange")
    Optional<LocalDate> findMinDateByAssetAndExchange(@Param("asset") Asset asset, @Param("exchange") Exchange exchange);

}
