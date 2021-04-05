package cz.tomastokamrazek.arion.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "date", "asset_id", "price_asset_id", "exchange_id" }))
public class Price {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @EqualsAndHashCode.Include
    @Column(nullable = false)
    LocalDate date;

    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Asset asset;
    
    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    Exchange exchange;
    
    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Asset priceAsset;
    
    @Column(nullable = true, precision = 38, scale = 18)
    BigDecimal openingPrice;
    
    @Column(nullable = true, precision = 38, scale = 18)
    BigDecimal lowPrice;
    
    @Column(nullable = true, precision = 38, scale = 18)
    BigDecimal highPrice;
    
    @Column(nullable = true, precision = 38, scale = 18)
    BigDecimal closingPrice;
    
    @Column(nullable = true, precision = 38, scale = 18)
    BigDecimal priceChange;
    
    @Column(nullable = true, precision = 38, scale = 18)
    BigDecimal volume;
    
    @Column(nullable = true, precision = 38, scale = 18)
    BigDecimal turnover;

}
