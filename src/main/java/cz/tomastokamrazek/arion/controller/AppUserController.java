package cz.tomastokamrazek.arion.controller;

import cz.tomastokamrazek.arion.dto.AppUserFioEbrokerRequest;
import cz.tomastokamrazek.arion.dto.AppUserRequest;
import cz.tomastokamrazek.arion.dto.AppUserResponse;
import cz.tomastokamrazek.arion.entity.AppUser;
import cz.tomastokamrazek.arion.security.HasAdminAuthority;
import cz.tomastokamrazek.arion.security.HasAnyAuthority;
import cz.tomastokamrazek.arion.service.AppUserService;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class AppUserController {
    
    @Autowired
    AppUserService appUserService;

    @Autowired
    ModelMapper modelMapper;

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

    @HasAdminAuthority
    @GetMapping("/{id}")
    public AppUserResponse getUserById(@RequestParam Long id) {
        return modelMapper.map(appUserService.getUser(id), AppUserResponse.class);
    }
    
    @HasAnyAuthority
    @PatchMapping("/{id}/fio-ebroker-credentials")
    public void updateFioEbrokerCredentials(@RequestBody AppUserFioEbrokerRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(username);
        appUserService.updateFioEbrokerCredentials(user, request.getUsername(), request.getPassword());
    }

    @HasAdminAuthority
    @PatchMapping("/{id}/disable")
    public void disable(@RequestParam Long id) {
        AppUser user = appUserService.getUser(id);
        appUserService.disableUser(user);
    }
    
    //TODO roles
    
}
