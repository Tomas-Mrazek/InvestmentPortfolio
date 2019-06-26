package cz.jaktoviditoka.investmentportfolio.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.*;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"date","asset_id","exchange_id"}))
public class AssetPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;  
    
    @Column(nullable=false)
    LocalDate date;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable=false)
    Asset asset;
    
    @Column(nullable=false)
    BigDecimal closingPrice;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable=false)
    Exchange exchange;  
    
}
