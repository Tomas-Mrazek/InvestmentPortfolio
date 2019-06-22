package cz.jaktoviditoka.investmentscraper.entity;

import cz.jaktoviditoka.investmentscraper.domain.AssetType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

import javax.persistence.*;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true, nullable = false)
    String name;

    @Column(unique = true, nullable = false)
    String ticker;

    @Column(unique = true)
    String scraperId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AssetType type;

    @ManyToMany
    List<Exchange> exchanges;

}
