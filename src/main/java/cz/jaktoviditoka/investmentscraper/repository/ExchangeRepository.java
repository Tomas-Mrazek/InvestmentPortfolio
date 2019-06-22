package cz.jaktoviditoka.investmentscraper.repository;

import cz.jaktoviditoka.investmentscraper.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExchangeRepository extends JpaRepository<Exchange, Long> {

}
