package cz.jaktoviditoka.investmentportfolio.controller;

import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetGroupedDto;
import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetPerDayValueDto;
import cz.jaktoviditoka.investmentportfolio.entity.AppUser;
import cz.jaktoviditoka.investmentportfolio.security.HasAnyAuthority;
import cz.jaktoviditoka.investmentportfolio.service.AppUserService;
import cz.jaktoviditoka.investmentportfolio.service.PortfolioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/portfolio")
public class PortfolioController {

    @Autowired
    AppUserService appUserService;
    
    @Autowired
    PortfolioService portfolioService;
    
    @HasAnyAuthority
    @GetMapping
    public List<PortfolioAssetGroupedDto> getPortfolio() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        return portfolioService.getPortfolio(user.getId());
    }
    
    @HasAnyAuthority
    @GetMapping("/day")
    public List<PortfolioAssetGroupedDto> getPortfolioPerDay() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        return portfolioService.getPortfolioPerDay(user.getId());
    }
    
    @HasAnyAuthority
    @GetMapping("/day/value")
    public List<PortfolioAssetPerDayValueDto> getPortfolioPerDayValue() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        return portfolioService.getPortfolioPerDayValue(user.getId());
    }
    
}
