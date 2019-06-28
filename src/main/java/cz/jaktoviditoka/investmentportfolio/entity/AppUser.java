package cz.jaktoviditoka.investmentportfolio.entity;

import cz.jaktoviditoka.investmentportfolio.dto.RoleType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

import javax.persistence.*;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(nullable = false)
    String firstName;
    
    @Column(nullable = false)
    String lastName;
    
    @Column(unique = true, nullable = false)
    String email;
    
    @Column(nullable = false)
    String password;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    List<RoleType> roles;
    
    @Column(nullable = false)
    Boolean passwordExpired;
    
    @Column(nullable = false)
    Boolean disabled;
    
    String fioEbrokerUsername;
    
    String fioEbrokerPassword;
    
}
