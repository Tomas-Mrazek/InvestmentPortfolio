package cz.jaktoviditoka.investmentportfolio.entity;

import cz.jaktoviditoka.investmentportfolio.domain.TransactionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(nullable = false)
    LocalDateTime timestamp;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    AppUser user;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TransactionType type;
       
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn
    TransactionPart in;
    
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn
    TransactionPart out;
    
    String comment;
    
    @Column(nullable = false)
    Boolean imported;
    
}
