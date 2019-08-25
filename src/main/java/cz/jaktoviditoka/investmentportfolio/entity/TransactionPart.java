package cz.jaktoviditoka.investmentportfolio.entity;

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
public class TransactionPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Asset asset;
    
    @Column(nullable = false, precision = 38, scale = 18)
    BigDecimal amount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    Asset feeAsset;
    
    @Column(precision = 38, scale = 18)
    BigDecimal feeAmount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    Asset sourceAsset;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Location location;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    Exchange exchange;
    
}
