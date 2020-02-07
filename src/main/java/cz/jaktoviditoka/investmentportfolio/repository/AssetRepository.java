package cz.jaktoviditoka.investmentportfolio.repository;

import cz.jaktoviditoka.investmentportfolio.domain.AssetType;
import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>{
    
    Optional<Asset> findByTicker(String name);
    
    Optional<Asset> findByIsin(String isin);
    
    List<Asset> findByType(AssetType type);
    
}
