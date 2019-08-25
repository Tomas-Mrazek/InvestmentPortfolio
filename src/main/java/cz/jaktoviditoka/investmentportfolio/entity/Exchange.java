package cz.jaktoviditoka.investmentportfolio.entity;

import cz.jaktoviditoka.investmentportfolio.domain.ExchangeAbbrEnum;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.persistence.*;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(unique = true, nullable = false)
    String name;
    
    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    ExchangeAbbrEnum abbreviation;

}
