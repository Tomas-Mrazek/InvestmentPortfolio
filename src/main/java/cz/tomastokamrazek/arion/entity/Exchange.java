package cz.tomastokamrazek.arion.entity;

import cz.tomastokamrazek.arion.domain.ExchangeAbbrEnum;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters.ZoneIdConverter;

import java.time.LocalTime;
import java.time.ZoneId;

import javax.persistence.*;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "exchange_generator")
    @SequenceGenerator(name="exchange_generator", sequenceName = "exchange_id_seq", allocationSize = 1)
    Long id;
    
    @Column(nullable = false, unique = true)
    String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    ExchangeAbbrEnum abbreviation;
    
    @Column(nullable = true)
    LocalTime openingTime;
    
    @Column(nullable = true)
    LocalTime closingTime;
    
    @Convert(converter = ZoneIdConverter.class)
    @Column(nullable = true)
    ZoneId timezone;

}
