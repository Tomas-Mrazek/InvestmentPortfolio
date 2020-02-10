package cz.jaktoviditoka.investmentportfolio.controller;

import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetPerDayResponse;
import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetResponse;
import cz.jaktoviditoka.investmentportfolio.entity.AppUser;
import cz.jaktoviditoka.investmentportfolio.security.HasAnyAuthority;
import cz.jaktoviditoka.investmentportfolio.service.AppUserService;
import cz.jaktoviditoka.investmentportfolio.service.PortfolioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    @Autowired
    AppUserService appUserService;

    @Autowired
    PortfolioService portfolioService;

    @HasAnyAuthority
    @GetMapping("/day")
    public List<PortfolioAssetPerDayResponse> getPortfolioPerDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> date) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        if(date.isPresent()) {
            return portfolioService.getPortfolioPerDay(user, date.get());
        } else {
            return portfolioService.getPortfolioPerDay(user);
        }
    }

    @HasAnyAuthority
    @GetMapping("/day-test")
    public List<PortfolioAssetResponse> getPortfolioPerDayTest() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        return portfolioService.getPortfolioPerDayTest(user);
    }
    
    @HasAnyAuthority
    @GetMapping("/amount-invested")
    public BigDecimal getAmountInvested() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        return portfolioService.amountInvested(user);
    }    
    
    @HasAnyAuthority
    @GetMapping("/value")
    public BigDecimal getValue() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        return portfolioService.value(user);
    }    

}
