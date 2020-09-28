package cz.tomastokamrazek.arion.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.Date;

import javax.persistence.*;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class CookieCache {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cookie_cache_generator")
    @SequenceGenerator(name="cookie_cache_generator", sequenceName = "cookie_cache_id_seq", allocationSize = 1)
    Long id;
    
    @CreatedDate
    LocalDateTime createdAt;
    
    @Column
    String name;
    
    @Column
    String value;
    
    @Column
    String domain;
    
    @Column
    String path;
    
    @Column
    Date expires;
    
    @Column
    boolean secure;
    
    @Column
    boolean httponly;
    
}
