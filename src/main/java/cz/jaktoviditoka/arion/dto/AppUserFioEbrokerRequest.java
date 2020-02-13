package cz.jaktoviditoka.arion.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotEmpty;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppUserFioEbrokerRequest {

    @NotEmpty
    String username;
    
    @NotEmpty
    String password;
    
}
