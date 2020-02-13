package cz.jaktoviditoka.arion.repository;

import cz.jaktoviditoka.arion.entity.TransactionMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionMovementRepository extends JpaRepository<TransactionMovement, Long> {

}
