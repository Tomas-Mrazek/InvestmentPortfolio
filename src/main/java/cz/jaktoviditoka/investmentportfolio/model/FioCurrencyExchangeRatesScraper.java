package cz.jaktoviditoka.investmentportfolio.model;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow.CellIterator;
import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.AssetPriceHistory;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.repository.AssetPriceHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
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
    AssetPriceHistoryRepository assetPriceHistoryRepository;

    WebClient webClient = new WebClient();

    @Transactional
    public void scrape(Asset asset, Exchange exchange, LocalDate minDate) throws IOException, InterruptedException {

        HtmlPage site = webClient.getPage("https://www.fio.cz/akcie-investice/dalsi-sluzby-fio/devizove-konverze");

        LocalDate maxDate = LocalDate.now();
        List<LocalDate> existingDates = assetPriceHistoryRepository.findByAssetAndExchange(asset, exchange).stream()
                .map(mapper -> mapper.getDate())
                .collect(Collectors.toList());
        List<LocalDate> processingDates = minDate.datesUntil(maxDate)
                .collect(Collectors.toList());
        processingDates.removeAll(existingDates);

        for (LocalDate date : processingDates) {
            log.debug("Scraping date: {}", date);

            HtmlTextInput dateInput = site.getElementByName("keDni_den");
            HtmlSubmitInput submit = site.getElementByName("keDni_submit");
            dateInput.type(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
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

                    BigDecimal rate = new BigDecimal(iterator.next().asText().replace(",", "."));

                    AssetPriceHistory assetPriceHistory = new AssetPriceHistory();
                    assetPriceHistory.setDate(date);
                    assetPriceHistory.setAsset(asset);
                    assetPriceHistory.setExchange(exchange);
                    assetPriceHistory.setClosingPrice(rate);

                    assetPriceHistoryRepository.save(assetPriceHistory);

                    log.debug("Scraping price history: {}", assetPriceHistory);
                }
            }

            Thread.sleep(new Random().nextInt(200) + 1000l);
        }

    }

}
