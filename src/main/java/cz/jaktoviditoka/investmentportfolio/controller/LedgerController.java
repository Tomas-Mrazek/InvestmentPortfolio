package cz.jaktoviditoka.investmentportfolio.controller;

import cz.jaktoviditoka.investmentportfolio.entity.AppUser;
import cz.jaktoviditoka.investmentportfolio.entity.Ledger;
import cz.jaktoviditoka.investmentportfolio.service.AppUserService;
import cz.jaktoviditoka.investmentportfolio.service.LedgerService;
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
    public List<Ledger> getEntries() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser appUser = appUserService.getUser(username);
        return ledgerService.getEntries(appUser);
    }
    
}
