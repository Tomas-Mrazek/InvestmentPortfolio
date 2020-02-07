package cz.jaktoviditoka.investmentportfolio.model;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow.CellIterator;
import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.entity.Price;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.PriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FioCurrencyExchangeRatesScraper {

    @Autowired
    AssetRepository assetRepository;
    
    @Autowired
    PriceRepository priceRepository;

    WebClient webClient = new WebClient();
    
    private static final String URL = "https://www.fio.cz/akcie-investice/dalsi-sluzby-fio/devizove-konverze";
    private static final String PRICE_ASSET = "CZK";

    @Transactional
    public void scrape(Asset asset, Exchange exchange, LocalDate scrapeDate) throws IOException, InterruptedException {
        log.trace("Scraping...");
        
        Asset priceAsset = assetRepository.findByTicker(PRICE_ASSET)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        List<LocalDate> existingDates = priceRepository.findByAssetAndPriceAssetAndExchange(asset, priceAsset, exchange).stream()
                .map(mapper -> mapper.getDate())
                .collect(Collectors.toList());
        
        List<LocalDate> missingDates = scrapeDate.datesUntil(LocalDate.now())
                .filter(el -> BooleanUtils.isNotTrue(existingDates.contains(el)))
                .collect(Collectors.toList());
        
        HtmlPage page = webClient.getPage(URL);
        
        for (LocalDate date : missingDates) {
            log.debug("Scraping date: {}", date);

            HtmlTextInput dateInput = page.getElementByName("keDni_den");
            HtmlSubmitInput submit = page.getElementByName("keDni_submit");
            dateInput.setText(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            HtmlPage result = submit.click();
            HtmlTable table = (HtmlTable) result.getByXPath(".//table[@class='tbl-sazby']").get(0);
            HtmlTableBody body = table.getBodies().get(0);
            List<HtmlTableRow> rows = body.getByXPath(".//tr[@class='odd' or @class='even']");
            for (HtmlTableRow row : rows) {               
                CellIterator iterator = row.getCellIterator();
                if (Objects.equals(iterator.next().asText(), asset.getTicker())) {                    
                    iterator.next();
                    iterator.next();
                    iterator.next();

                    BigDecimal sellPriceValue = new BigDecimal(iterator.next().asText().replace(",", "."));
                    BigDecimal buyPriceValue = new BigDecimal(iterator.next().asText().replace(",", "."));

                    Price sellPrice = Price.builder()
                            .date(date)
                            .asset(asset)
                            .closingPrice(sellPriceValue)
                            .priceAsset(priceAsset)
                            .exchange(exchange)
                            .build();
                    priceRepository.save(sellPrice);
                    
                    Price buzPrice = Price.builder()
                            .date(date)
                            .asset(priceAsset)
                            .closingPrice(BigDecimal.ONE.divide(buyPriceValue, 5, RoundingMode.HALF_UP))
                            .priceAsset(asset)
                            .exchange(exchange)
                            .build();
                    priceRepository.save(buzPrice);
                }
            }

            Thread.sleep(new Random().nextInt(20) + 200l);
        }

        log.trace("Scraping finished...");
    }

}
