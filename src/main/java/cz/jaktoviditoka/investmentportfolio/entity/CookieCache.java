package cz.jaktoviditoka.investmentportfolio.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class CookieCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @CreatedDate
    LocalDateTime createdAt;
    
    String name;
    
    String value;
    
    String domain;
    
    String path;
    
    Date expires;
    
    boolean secure;
    
    boolean httponly;
    
}
