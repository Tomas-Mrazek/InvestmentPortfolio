package cz.jaktoviditoka.investmentscraper.repository;

import cz.jaktoviditoka.investmentscraper.entity.TransactionPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionPartRepository extends JpaRepository<TransactionPart, Long> {

}
