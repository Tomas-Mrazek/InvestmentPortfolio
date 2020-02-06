package cz.jaktoviditoka.investmentportfolio.entity;

import cz.jaktoviditoka.investmentportfolio.domain.RoleType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(nullable = false, unique = true)
    String username;
    
    @Column(nullable = false)
    String firstName;
    
    @Column(nullable = false)
    String lastName;
    
    @Column(nullable = false, unique = true)
    String email;
    
    @Column(nullable = false)
    String password;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    List<RoleType> roles;
    
    @Column(nullable = false)
    Boolean passwordExpired;
    
    @Column(nullable = false)
    Boolean disabled;
    
    @Column(nullable = true)
    String fioEbrokerUsername;
    
    @Column(nullable = true)
    String fioEbrokerPassword;
    
}
