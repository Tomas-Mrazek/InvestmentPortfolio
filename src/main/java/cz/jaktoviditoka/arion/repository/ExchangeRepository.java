package cz.jaktoviditoka.arion.repository;

import cz.jaktoviditoka.arion.domain.ExchangeAbbrEnum;
import cz.jaktoviditoka.arion.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRepository extends JpaRepository<Exchange, Long> {

    Optional<Exchange> findByAbbreviation(ExchangeAbbrEnum abbr);
    
}
