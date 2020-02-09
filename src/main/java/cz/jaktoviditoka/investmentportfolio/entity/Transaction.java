package cz.jaktoviditoka.investmentportfolio.entity;

import cz.jaktoviditoka.investmentportfolio.domain.TransactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.ZonedDateTime;

import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_generator")
    @SequenceGenerator(name="transaction_generator", sequenceName = "transaction_id_seq", allocationSize = 10)
    Long id;
    
    @Column(nullable = false)
    ZonedDateTime timestamp;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    AppUser user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TransactionType type;
       
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(nullable = true)
    TransactionMovement in;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(nullable = true)
    TransactionMovement out;
    
    @Column(nullable = true)
    String comment;
    
    @Column(nullable = false)
    Boolean imported;
    
}
