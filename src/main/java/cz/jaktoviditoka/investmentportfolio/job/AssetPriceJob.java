package cz.jaktoviditoka.investmentportfolio.job;

import cz.jaktoviditoka.investmentportfolio.domain.ExchangeAbbrEnum;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
public class AssetPriceJob {

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

    private static final String LOG_MESSAGE = "Scraping...\n'asset': {}\n'exchange': {}\n'minDate': {}\n'via': {}";

    // @Scheduled(fixedRate = 10 * 1000)
    public void createMissingRecords() throws IOException, InterruptedException {

        String baseCurrency = "CZK";

        List<Asset> assets = assetRepository.findAll();

        for (Asset asset : assets) {
            if (Objects.equals(asset.getTicker(), baseCurrency)) {
                continue;
            }
            for (Exchange exchange : asset.getExchanges()) {

                Optional<LocalDate> minDate = portfolioAssetRepository.findMinDateByAssetAndExchange(asset, exchange);
                log.trace("Scraping...\n'asset': {}\n'exchange': {}\n'minDate': {}", asset, exchange, minDate);
                if (minDate.isPresent()) {
                    if (Objects.equals(ExchangeAbbrEnum.BCPP, exchange.getAbbreviation())
                            || Objects.equals(ExchangeAbbrEnum.RMS, exchange.getAbbreviation())) {
                        log.debug(LOG_MESSAGE, asset, exchange, minDate.get(), "kurzyCzScraper");
                        kurzyCzScraper.scrape(asset, exchange, minDate.get());
                    } else if (Objects.equals(ExchangeAbbrEnum.FIO, exchange.getAbbreviation())) {
                        log.debug(LOG_MESSAGE, asset, exchange, minDate.get(), "fioCurrencyExchangeRatesScraper");
                        fioCurrencyExchangeRatesScraper.scrape(asset, exchange, minDate.get());
                    } else if (Objects.equals(ExchangeAbbrEnum.NYSE, exchange.getAbbreviation())) {
                        log.debug(LOG_MESSAGE, asset, exchange, minDate.get(), "alphaVantageClient");
                        alphaVantageClient.getAssetHistoricPrice(asset, exchange, minDate.get());
                    }
                }
            }
        }

    }

}
