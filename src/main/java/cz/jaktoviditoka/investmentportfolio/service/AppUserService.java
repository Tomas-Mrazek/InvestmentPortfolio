package cz.jaktoviditoka.investmentportfolio.service;

import cz.jaktoviditoka.investmentportfolio.domain.RoleType;
import cz.jaktoviditoka.investmentportfolio.dto.AppUserRequest;
import cz.jaktoviditoka.investmentportfolio.entity.AppUser;
import cz.jaktoviditoka.investmentportfolio.repository.AppUserRepository;
import cz.jaktoviditoka.investmentportfolio.security.PasswordCryptoProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppUserService implements UserDetailsService {

    //TODO Only for testing purpores
    private static final RoleType DEFAULT_ROLE = RoleType.ADMIN;

    @Autowired
    AppUserRepository appUserRepository;
    
    @Autowired
    PasswordCryptoProvider passwordCryptoProvider;

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials."));

        List<GrantedAuthority> authorities = appUser.getRoles().stream()
                .map(el -> new SimpleGrantedAuthority(el.name()))
                .collect(Collectors.toList());

        return User.builder()
                .username(appUser.getUsername())
                .password(appUser.getPassword())
                .authorities(authorities)
                .credentialsExpired(appUser.getPasswordExpired())
                .disabled(appUser.getDisabled())
                .build();
    }

    public void register(AppUserRequest appUserDto) {
        AppUser appUser = AppUser.builder()
                .username(appUserDto.getEmail())
                .firstName(appUserDto.getFirstName())
                .lastName(appUserDto.getLastName())
                .email(appUserDto.getEmail())
                .password(passwordCryptoProvider.encode(appUserDto.getPassword()))
                .roles(List.of(DEFAULT_ROLE))
                .passwordExpired(false)
                .disabled(false)
                .build();
        appUserRepository.save(appUser);
    }

    public List<AppUser> getUsers() {
        return appUserRepository.findAll();
    }

    public AppUser getUser(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public AppUser getUser(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    
    public AppUser updateFioEbrokerCredentials(AppUser appUser, String username, String password) {
        appUser.setFioEbrokerUsername(username);
        appUser.setFioEbrokerPassword(passwordCryptoProvider.encrypt(password));
        return appUserRepository.save(appUser);
    }

    public AppUser disableUser(AppUser appUser) {
        appUser.setDisabled(true);
        return appUserRepository.save(appUser);
    }

    public AppUser grantRole(RoleType role, AppUser appUser) {
        appUser.getRoles().add(role);
        return appUserRepository.save(appUser);
    }

    public AppUser grevokeRole(RoleType role, AppUser appUser) {
        appUser.getRoles().remove(role);
        return appUserRepository.save(appUser);
    }

}
