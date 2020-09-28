package cz.tomastokamrazek.arion.controller;

import cz.tomastokamrazek.arion.dto.LedgerResponse;
import cz.tomastokamrazek.arion.entity.AppUser;
import cz.tomastokamrazek.arion.service.AppUserService;
import cz.tomastokamrazek.arion.service.LedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ledger")
public class LedgerController {
    
    @Autowired
    AppUserService appUserService;
    
    @Autowired
    LedgerService ledgerService;

    @GetMapping
    public List<LedgerResponse> getEntries() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser appUser = appUserService.getUser(username);
        return ledgerService.getEntries(appUser);
    }
    
}
