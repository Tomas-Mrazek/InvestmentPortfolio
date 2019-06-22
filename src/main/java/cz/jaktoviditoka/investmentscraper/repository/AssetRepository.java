package cz.jaktoviditoka.investmentscraper.repository;

import cz.jaktoviditoka.investmentscraper.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>{

    Optional<Asset> findByName(String name);
    
    Optional<Asset> findByTicker(String name);
    
}
