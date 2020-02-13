package cz.jaktoviditoka.arion.controller;

import cz.jaktoviditoka.arion.dto.LedgerResponse;
import cz.jaktoviditoka.arion.entity.AppUser;
import cz.jaktoviditoka.arion.service.AppUserService;
import cz.jaktoviditoka.arion.service.LedgerService;
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
