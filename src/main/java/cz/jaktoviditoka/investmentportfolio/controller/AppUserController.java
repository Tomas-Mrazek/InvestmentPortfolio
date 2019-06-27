package cz.jaktoviditoka.investmentportfolio.controller;

import cz.jaktoviditoka.investmentportfolio.dto.AppUserFioEbrokerRequest;
import cz.jaktoviditoka.investmentportfolio.dto.AppUserRequest;
import cz.jaktoviditoka.investmentportfolio.dto.AppUserResponse;
import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetGroupedDto;
import cz.jaktoviditoka.investmentportfolio.entity.AppUser;
import cz.jaktoviditoka.investmentportfolio.security.HasAdminAuthority;
import cz.jaktoviditoka.investmentportfolio.security.HasAnyAuthority;
import cz.jaktoviditoka.investmentportfolio.security.PasswordCryptoProvider;
import cz.jaktoviditoka.investmentportfolio.service.AppUserService;
import cz.jaktoviditoka.investmentportfolio.service.PortfolioService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/users")
public class AppUserController {
    
    @Autowired
    AppUserService appUserService;
    
    @Autowired
    PortfolioService portfolioService;

    @Autowired
    ModelMapper modelMapper;
    
    @Autowired
    PasswordCryptoProvider passwordCryptoProvider;

    @PostMapping("/register")
    public void register(@RequestBody AppUserRequest user) {
        appUserService.register(user);
    }

    @HasAdminAuthority
    @GetMapping
    public List<AppUserResponse> getUsers(@RequestParam(required = false) String username) {
        if (StringUtils.isBlank(username)) {
            return appUserService.getUsers().stream()
                    .map(el -> modelMapper.map(el, AppUserResponse.class))
                    .collect(Collectors.toList());
        } else {
            return List.of(modelMapper.map(appUserService.getUser(username), AppUserResponse.class));
        }

    }
    
    @HasAnyAuthority
    @PatchMapping
    public void updateUser(@RequestBody AppUserFioEbrokerRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        user.setFioEbrokerUsername(request.getUsername());
        user.setFioEbrokerPassword(passwordCryptoProvider.encrypt(request.getPassword()));
        appUserService.updateUser(user);
    }

    @HasAdminAuthority
    @GetMapping("/{id}")
    public AppUserResponse getUserById(@RequestParam Long id) {
        return modelMapper.map(appUserService.getUser(id), AppUserResponse.class);
    }

    @HasAdminAuthority
    @PatchMapping("/{id}/disable")
    public void disable(@RequestParam Long id) {
        appUserService.disableUser(id);
    }
    
    @HasAnyAuthority
    @GetMapping("/portfolio")
    public List<PortfolioAssetGroupedDto> getPortfolio() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        return portfolioService.getPortfolio(user.getId());
    }
    
    @HasAnyAuthority
    @GetMapping("/portfolio/day")
    public List<PortfolioAssetGroupedDto> getPortfolioByDay() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        return portfolioService.getPortfolioByDay(user.getId());
    }
    
}
