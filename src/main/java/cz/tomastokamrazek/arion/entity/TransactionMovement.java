package cz.tomastokamrazek.arion.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class TransactionMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transaction_movement_generator")
    @SequenceGenerator(name="transaction_movement_generator", sequenceName = "transaction_movement_id_seq", allocationSize = 20)
    Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    Exchange exchange;
    
    @Column(nullable = true)
    String location;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Asset asset;
    
    @Column(nullable = false, precision = 38, scale = 18)
    BigDecimal amount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    Asset feeAsset;
    
    @Column(nullable = true, precision = 38, scale = 18)
    BigDecimal feeAmount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    Asset sourceAsset;
    
}
