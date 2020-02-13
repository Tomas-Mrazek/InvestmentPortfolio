package cz.jaktoviditoka.arion.repository;

import cz.jaktoviditoka.arion.domain.AssetType;
import cz.jaktoviditoka.arion.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>{
    
    Optional<Asset> findByIsin(String isin);
    
    Optional<Asset> findByTicker(String ticker);
    
    List<Asset> findByType(AssetType type);
    
}
