package cz.jaktoviditoka.investmentportfolio.job;

import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.model.KurzyCzScraper;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.PortfolioAssetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class AssetPriceHistoryJob {

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    PortfolioAssetRepository portfolioAssetRepository;

    @Autowired
    KurzyCzScraper scraper;

    // @Scheduled(fixedRate = 10 * 1000)
    public void createMissingRecords() throws IOException, InterruptedException {

        List<Asset> assets = assetRepository.findAll();

        for (Asset asset : assets) {
            for (Exchange exchange : asset.getExchanges()) {
                Optional<LocalDate> minDate = portfolioAssetRepository.findMinDateByAssetAndExchange(asset, exchange);
                if (minDate.isPresent()) {
                    log.debug("Scraping 'asset': {} | 'exchange': {} | 'minDate': {}", asset, exchange, minDate);
                    scraper.scrape(asset, exchange, minDate.get());
                }
            }
        }

    }

}
