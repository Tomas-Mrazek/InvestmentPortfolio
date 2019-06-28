package cz.jaktoviditoka.investmentportfolio.service;

import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetGroupedDto;
import cz.jaktoviditoka.investmentportfolio.dto.PortfolioAssetPerDayValueDto;
import cz.jaktoviditoka.investmentportfolio.model.Portfolio;
import cz.jaktoviditoka.investmentportfolio.repository.PortfolioAssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PortfolioService {

    @Autowired
    PortfolioAssetRepository portfolioAssetRepository;
    
    @Autowired
    Portfolio portfolio;
    
    public List<PortfolioAssetGroupedDto> getPortfolio(Long userId) {
        return portfolioAssetRepository.findAllGroupedPerDay(userId);
    }
    
    public List<PortfolioAssetGroupedDto> getPortfolioPerDay(Long userId) {
        return portfolio.portfolioPerDay(userId);
    }
    
    public List<PortfolioAssetPerDayValueDto> getPortfolioPerDayValue(Long userId) {
        return portfolio.portfolioPerDayValue(userId);
    }
    
}
