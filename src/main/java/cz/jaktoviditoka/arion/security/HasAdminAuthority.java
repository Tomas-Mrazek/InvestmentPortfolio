package cz.jaktoviditoka.arion.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority(T(cz.jaktoviditoka.arion.domain.RoleType).ADMIN.name())")
public @interface HasAdminAuthority {

}
