package cz.jaktoviditoka.investmentportfolio.dto;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotEmpty;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppUserRequest {

    String firstName;

    String lastName;

    @NotEmpty
    CharSequence password;
    
    @NotEmpty
    CharSequence matchingPassword;

    @NotEmpty
    String email;

}
