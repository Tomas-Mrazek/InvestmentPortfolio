package cz.jaktoviditoka.investmentportfolio.job;

import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.model.AlphaVantageClient;
import cz.jaktoviditoka.investmentportfolio.model.FioCurrencyExchangeRatesScraper;
import cz.jaktoviditoka.investmentportfolio.model.KurzyCzScraper;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.PortfolioAssetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class AssetPriceHistoryJob {

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    PortfolioAssetRepository portfolioAssetRepository;

    @Autowired
    AlphaVantageClient alphaVantageClient;

    @Autowired
    KurzyCzScraper kurzyCzScraper;

    @Autowired
    FioCurrencyExchangeRatesScraper fioCurrencyExchangeRatesScraper;

    // @Scheduled(fixedRate = 10 * 1000)
    @Transactional
    public void createMissingRecords() throws IOException, InterruptedException {

        String baseCurrency = "CZK";

        List<Asset> assets = assetRepository.findAll();

        for (Asset asset : assets) {
            if (Objects.equals(asset.getTicker(), baseCurrency)) {
                continue;
            }
            for (Exchange exchange : asset.getExchanges()) {

                Optional<LocalDate> minDate = portfolioAssetRepository.findMinDateByAssetAndExchange(asset,
                        exchange);
                if (minDate.isPresent()) {
                    if (exchange.getName().contentEquals("BCPP") || exchange.getName().contentEquals("RMS")) {
                        log.debug("Scraping...\n'asset': {}\n'exchange': {}\n'minDate': {}\n'via': {}",
                                asset, exchange, minDate.get(), "kurzyCzScraper");
                        kurzyCzScraper.scrape(asset, exchange, minDate.get());
                    } else if (exchange.getName().contentEquals("Fio Banka")) {
                        log.debug("Scraping...\n'asset': {}\n'exchange': {}\n'minDate': {}\n'via': {}",
                                asset, exchange, minDate.get(), "fioCurrencyExchangeRatesScraper");
                        fioCurrencyExchangeRatesScraper.scrape(asset, exchange, minDate.get());
                    } else if (exchange.getName().contentEquals("NYSE")) {
                        log.debug("Scraping...\n'asset': {}\n'exchange': {}\n'minDate': {}\n'via': {}",
                                asset, exchange, minDate.get(), "alphaVantageClient");
                        alphaVantageClient.getAssetHistoricPrice(asset, exchange, minDate.get());
                    }
                }
            }
        }

    }

}
