package cz.jaktoviditoka.investmentportfolio.controller;

import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetPerDayResponse;
import cz.jaktoviditoka.investmentportfolio.dto.PortfolioDayValueDto;
import cz.jaktoviditoka.investmentportfolio.dto.PortfolioHistoryDayDto;
import cz.jaktoviditoka.investmentportfolio.entity.AppUser;
import cz.jaktoviditoka.investmentportfolio.security.HasAnyAuthority;
import cz.jaktoviditoka.investmentportfolio.service.AppUserService;
import cz.jaktoviditoka.investmentportfolio.service.PortfolioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
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
    @GetMapping("/history")
    public List<PortfolioHistoryDayDto> portfolioHistory() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        return portfolioService.portfolioHistory(user);
    }
    
    @HasAnyAuthority
    @GetMapping("/value")
    public List<PortfolioDayValueDto> portfolioValue() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        return portfolioService.portfolioValue(user);
    }
    
    @HasAnyAuthority
    @GetMapping("/value/graph")
    public ResponseEntity<byte[]> portfolioValueGraph() throws InterruptedException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser user = appUserService.getUser(email);
        byte[] array = portfolioService.portfolioValuePrint(user).toByteArray();
        log.debug("byte array size: {}", array.length);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(array);
    }

}
