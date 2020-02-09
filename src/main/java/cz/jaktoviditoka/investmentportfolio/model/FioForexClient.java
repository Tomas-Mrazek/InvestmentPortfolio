package cz.jaktoviditoka.investmentportfolio.model;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow.CellIterator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FioForexClient {

    WebClient webClient = new WebClient();

    private static final String URL = "https://www.fio.cz/akcie-investice/dalsi-sluzby-fio/devizove-konverze";

    @Transactional
    public List<FioForexPrice> importPrice(LocalDate date) throws IOException {
        log.debug("Importing – {}", date.format(DateTimeFormatter.ofPattern("d.M.yyyy")));

        List<FioForexPrice> listOfPrices = new ArrayList<>();

        HtmlPage page = webClient.getPage(URL);
        HtmlTextInput dateInput = page.getElementByName("keDni_den");
        HtmlSubmitInput submit = page.getElementByName("keDni_submit");
        dateInput.setText(date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        HtmlPage result = submit.click();
        HtmlTable table = (HtmlTable) result.getByXPath(".//table[@class='tbl-sazby']").get(0);
        HtmlTableBody body = table.getBodies().get(0);
        List<HtmlTableRow> rows = body.getByXPath(".//tr[@class='odd' or @class='even']");
        for (HtmlTableRow row : rows) {
            CellIterator iterator = row.getCellIterator();
            FioForexPrice price = FioForexPrice.builder()
                    .date(date.format(DateTimeFormatter.ISO_DATE))
                    .asset(iterator.next().asText())
                    .country(iterator.next().asText())
                    .amount(iterator.next().asText())
                    .buyPrice(iterator.next().asText())
                    .sellPrice(iterator.next().asText())
                    .cnbPrice(iterator.next().asText())
                    .build();

            listOfPrices.add(price);
        }

        return listOfPrices;
    }

}
