package cz.tomastokamrazek.arion.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@HasAdminAuthority
@HasUserAuthority
public @interface HasAnyAuthority {

}
