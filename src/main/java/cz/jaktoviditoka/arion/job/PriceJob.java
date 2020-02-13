package cz.jaktoviditoka.arion.job;

import cz.jaktoviditoka.arion.entity.Asset;
import cz.jaktoviditoka.arion.model.AlphaVantageClient;
import cz.jaktoviditoka.arion.model.FioForexClient;
import cz.jaktoviditoka.arion.repository.AssetRepository;
import cz.jaktoviditoka.arion.repository.LedgerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class PriceJob {

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    LedgerRepository ledgerRepository;

    @Autowired
    AlphaVantageClient alphaVantageClient;

    @Autowired
    FioForexClient fioCurrencyExchangeRatesScraper;

    private static final String BASE_CURRENCY = "CZK";
    private static final String LOG_MESSAGE = "Scraping...\n'asset': {}\n'exchange': {}\n'minDate': {}\n'via': {}";

    // @Scheduled(fixedRate = 10 * 1000)
    public void createMissingRecords() throws IOException, InterruptedException {

        List<Asset> assets = assetRepository.findAll();

        for (Asset asset : assets) {
            if (Objects.equals(asset.getTicker(), BASE_CURRENCY)) {
                continue;
            }
            //createMissingRecords(asset, exchange);
        }

    }

    /*
    public void createMissingRecords(Asset asset, Exchange exchange) throws IOException, InterruptedException {
        if (Objects.equals(asset.getTicker(), BASE_CURRENCY)) {
            return;
        }
        Optional<LocalDate> minDate = ledgerRepository.findMinDateByAssetAndExchange(asset, exchange);
        if (minDate.isPresent()) {
            if (Objects.equals(exchange.getAbbreviation(), ExchangeAbbrEnum.FIO)) {
                log.debug(LOG_MESSAGE, asset, exchange, minDate.get(), "fioCurrencyExchangeRatesScraper");
                fioCurrencyExchangeRatesScraper.scrape(asset, exchange, minDate.get());
            } else if (Objects.equals(exchange.getAbbreviation(), ExchangeAbbrEnum.NYSE)) {
                log.debug(LOG_MESSAGE, asset, exchange, minDate.get(), "alphaVantageClient");
                alphaVantageClient.getAssetHistoricPrice(asset, exchange, minDate.get());
            }
        }
    }
    */

}
