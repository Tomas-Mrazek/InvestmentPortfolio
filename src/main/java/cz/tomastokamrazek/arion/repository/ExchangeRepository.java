package cz.tomastokamrazek.arion.repository;

import cz.tomastokamrazek.arion.domain.ExchangeAbbrEnum;
import cz.tomastokamrazek.arion.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExchangeRepository extends JpaRepository<Exchange, Long> {

    Optional<Exchange> findByAbbreviation(ExchangeAbbrEnum abbr);
    
}
