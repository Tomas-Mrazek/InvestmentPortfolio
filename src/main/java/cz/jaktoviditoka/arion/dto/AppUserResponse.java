package cz.jaktoviditoka.arion.dto;

import cz.jaktoviditoka.arion.domain.RoleType;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppUserResponse {

    Long id;
    
    String firstName;

    String lastName;

    String email;
    
    List<RoleType> roles;
    
    boolean passwordExpired;
    
    boolean disabled;

}
