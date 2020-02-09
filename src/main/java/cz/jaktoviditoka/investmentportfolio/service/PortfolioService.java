package cz.jaktoviditoka.investmentportfolio.service;

import cz.jaktoviditoka.investmentportfolio.domain.PortfolioAssetPerDay;
import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetPerDayResponse;
import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetResponse;
import cz.jaktoviditoka.investmentportfolio.entity.AppUser;
import cz.jaktoviditoka.investmentportfolio.model.PortfolioManagement;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Transactional
@Service
public class PortfolioService {

    @Autowired
    PortfolioManagement portfolio;

    @Autowired
    ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<PortfolioAssetPerDayResponse> getPortfolioPerDay(AppUser appuser) {
        List<PortfolioAssetPerDay> listOfPaapd = portfolio.portfolioPerDay(appuser);
        List<PortfolioAssetPerDayResponse> listOfPaapdDto = new ArrayList<>();

        listOfPaapd.stream().forEach(el -> {
            PortfolioAssetPerDayResponse paapdDto = PortfolioAssetPerDayResponse.builder()
                    .date(el.getDate())
                    .assets(new ArrayList<>())
                    .build();

            el.getAssets().stream().forEach(asset -> {
                paapdDto.getAssets().add(modelMapper.map(asset, PortfolioAssetResponse.class));
            });

            paapdDto.getAssets().sort(Comparator
                    .comparing(PortfolioAssetResponse::getAssetType)
                    .thenComparing(PortfolioAssetResponse::getAssetName));

            listOfPaapdDto.add(paapdDto);
        });

        return listOfPaapdDto;
    }

    @Transactional(readOnly = true)
    public List<PortfolioAssetResponse> getPortfolioPerDayTest(AppUser appuser) {
        return portfolio.portfolioPerDayTest(appuser);
    }

    @Transactional(readOnly = true)
    public BigDecimal amountInvested(AppUser appuser) {
        return portfolio.amountInvested(appuser);
    }

    @Transactional(readOnly = true)
    public BigDecimal value(AppUser appuser) {
        return portfolio.value(appuser);
    }
    
}
