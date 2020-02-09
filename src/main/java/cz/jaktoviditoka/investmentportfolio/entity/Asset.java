package cz.jaktoviditoka.investmentportfolio.entity;

import cz.jaktoviditoka.investmentportfolio.domain.AssetType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "ticker", "isin", "type" }) })
@Entity
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "asset_generator")
    @SequenceGenerator(name="asset_generator", sequenceName = "asset_id_seq", allocationSize = 20)
    Long id;

    @EqualsAndHashCode.Include
    @Column(nullable = false)
    String name;

    @EqualsAndHashCode.Include
    @Column(nullable = true, unique = true)
    String ticker;

    @EqualsAndHashCode.Include
    @Column(nullable = true, unique = true)
    String isin;

    @EqualsAndHashCode.Include
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AssetType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = true)
    Asset nominalPriceAsset;

    @Column(nullable = true, precision = 38, scale = 18)
    BigDecimal nominalPrice;

}
