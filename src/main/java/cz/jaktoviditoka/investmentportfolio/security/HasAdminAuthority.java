package cz.jaktoviditoka.investmentportfolio.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority(T(cz.jaktoviditoka.investmentportfolio.domain.RoleType).ADMIN.name())")
public @interface HasAdminAuthority {

}
