package cz.jaktoviditoka.investmentportfolio.repository;

import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetGroupedDto;
import cz.jaktoviditoka.investmentportfolio.entity.*;
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

    List<PortfolioAsset> findByUserAndAsset(AppUser user, Asset asset);

    List<PortfolioAsset> findByUserAndAssetAndLocation(AppUser user, Asset asset, Location location);
    
    @Query("SELECT new cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetGroupedDto(date, asset.id, sum(amount), exchange.id, location.id)"
            + " FROM PortfolioAsset pa WHERE user.id = :userId GROUP BY user.id, date, asset.id, exchange.id, location.id ORDER BY date")
    List<PortfolioAssetGroupedDto> findAllGroupedPerDay(@Param("userId") Long userId);
    
    @Query("SELECT min(date) FROM PortfolioAsset WHERE asset = :asset AND exchange = :exchange")
    Optional<LocalDate> findMinDateByAssetAndExchange(@Param("asset") Asset asset, @Param("exchange") Exchange exchange);

}
