package cz.jaktoviditoka.investmentportfolio.entity;

import cz.jaktoviditoka.investmentportfolio.domain.AssetType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

import javax.persistence.*;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String name;

    @Column(nullable = false, unique = true)
    String ticker;

    @Column(nullable = true, unique = true)
    String scraperId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AssetType type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    Asset nominalPriceAsset;
    
    @Column(nullable = true, precision = 38, scale = 18)
    BigDecimal nominalPrice;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    List<Exchange> exchanges;

}
