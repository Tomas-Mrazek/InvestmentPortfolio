package cz.jaktoviditoka.investmentportfolio.entity;

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
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "date", "asset_id", "price_asset_id", "exchange_id" }))
public class AssetPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Asset asset;

    @Column(nullable = false, precision = 38, scale = 18)
    BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Asset priceAsset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Exchange exchange;

}
