package cz.jaktoviditoka.investmentportfolio.repository;

import cz.jaktoviditoka.investmentportfolio.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId);
    
    List<Transaction> findByUserEmail(String username);
    
}
