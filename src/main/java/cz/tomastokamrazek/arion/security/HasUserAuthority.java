package cz.tomastokamrazek.arion.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority(T(cz.tomastokamrazek.arion.domain.RoleType).USER.name())")
public @interface HasUserAuthority {

}
