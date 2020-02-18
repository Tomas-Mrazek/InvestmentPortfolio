package cz.jaktoviditoka.arion.repository;

import cz.jaktoviditoka.arion.entity.AppUser;
import cz.jaktoviditoka.arion.entity.Asset;
import cz.jaktoviditoka.arion.entity.Exchange;
import cz.jaktoviditoka.arion.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerRepository extends JpaRepository<Ledger, Long> {
    
    List<Ledger> findByUser(AppUser user);
    
    List<Ledger> findByUserEmail(String userEmail);

    List<Ledger> findByUserAndAsset(AppUser user, Asset asset);

    List<Ledger> findByUserAndAssetAndLocation(AppUser user, Asset asset, String location);
    
    @Query("SELECT min(timestamp) FROM Ledger WHERE asset = :asset AND exchange = :exchange")
    Optional<LocalDate> findMinDateByAssetAndExchange(@Param("asset") Asset asset, @Param("exchange") Exchange exchange);

}