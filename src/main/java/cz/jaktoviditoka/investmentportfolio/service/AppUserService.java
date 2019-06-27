package cz.jaktoviditoka.investmentportfolio.service;

import cz.jaktoviditoka.investmentportfolio.domain.RoleType;
import cz.jaktoviditoka.investmentportfolio.dto.AppUserRequest;
import cz.jaktoviditoka.investmentportfolio.entity.AppUser;
import cz.jaktoviditoka.investmentportfolio.repository.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

@Slf4j
@Service
public class AppUserService implements UserDetailsService {
    
    private static RoleType DEFAULT_ROLE = RoleType.USER;
    
    @Autowired
    AppUserRepository appUserRepository;
    
    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser appUser = appUserRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials."));

        List<GrantedAuthority> authorities = appUser.getRoles().stream()
                .map(el -> new SimpleGrantedAuthority(el.name()))
                .collect(Collectors.toList());

        return User.builder()
                .username(appUser.getEmail())
                .password(appUser.getPassword())
                .authorities(authorities)
                .credentialsExpired(appUser.getPasswordExpired())
                .disabled(appUser.getDisabled())
                .build();
    }
    
    public void register(AppUserRequest appUserDto) {
        AppUser appUser = new AppUser();
        appUser.setFirstName(appUserDto.getFirstName());
        appUser.setLastName(appUserDto.getLastName());
        appUser.setEmail(appUserDto.getEmail());
        appUser.setPassword(passwordEncoder.encode(appUserDto.getPassword()));
        appUser.setRoles(List.of(DEFAULT_ROLE));
        appUser.setPasswordExpired(false);
        appUser.setDisabled(false);
        appUserRepository.save(appUser);
    }
    
    public List<AppUser> getUsers() {
        return appUserRepository.findAll();
    }
    
    public AppUser getUser(Long id) {
        return appUserRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    
    public AppUser getUser(String username) {
        return appUserRepository.findByEmail(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    
    public void updateUser(AppUser user) {
        appUserRepository.save(user);
    }
    
    public void disableUser(Long id) {
        AppUser appUser = appUserRepository.findById(id).orElseThrow(() -> new EntityNotFoundException());
        appUser.setDisabled(true);
        appUserRepository.save(appUser);
    }
    
    public List<RoleType> getRoles() {
        return List.of(RoleType.values());
    }

    public void grantRole(RoleType role, String username) {
        AppUser appUser = appUserRepository.findByEmail(username).orElseThrow(() -> new EntityNotFoundException());
        appUser.getRoles().add(role);
        appUserRepository.save(appUser);
    }
    
    public void grevokeRole(RoleType role, String username) {
        AppUser appUser = appUserRepository.findByEmail(username).orElseThrow(() -> new EntityNotFoundException());
        appUser.getRoles().remove(role);
        appUserRepository.save(appUser);
    }

}
