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
public class PortfolioAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    AppUser user;
    
    @Column(nullable = false)
    LocalDate date;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false)
    Asset asset;
    
    @Column(nullable = false)
    BigDecimal amount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    Exchange exchange;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    Location location;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn
    Transaction transaction;

}
