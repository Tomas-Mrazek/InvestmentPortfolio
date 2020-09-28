package cz.tomastokamrazek.arion.repository;

import cz.tomastokamrazek.arion.entity.TransactionMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionMovementRepository extends JpaRepository<TransactionMovement, Long> {

}
