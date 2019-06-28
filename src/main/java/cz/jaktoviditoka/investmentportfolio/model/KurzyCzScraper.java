package cz.jaktoviditoka.investmentportfolio.model;

import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.AssetPriceHistory;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.repository.AssetPriceHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class KurzyCzScraper {

    @Autowired
    AssetPriceHistoryRepository assetPriceHistoryRepository;

    private static final int PAGE_SIZE = 100;

    String urlTemplate = "https://akcie-cz.kurzy.cz/prehled.asp?T=PK&CP=${assetId}&MAXROWS=${pageSize}&RF=${firstRow}";

    @Transactional
    public void scrape(Asset asset, Exchange exchange, LocalDate minDate) throws IOException, InterruptedException {
        log.debug("Scraping...");

        Optional<LocalDate> maxDateOpt = assetPriceHistoryRepository.findByAssetAndExchange(asset, exchange).stream()
                .map(mapper -> mapper.getDate())
                .min(Comparator.comparing(LocalDate::toEpochDay));

        LocalDate maxDate;

        if (maxDateOpt.isPresent()) {
            maxDate = maxDateOpt.get();
        } else {
            maxDate = LocalDate.now();
        }

        if (BooleanUtils.isNotTrue(maxDate.isAfter(minDate))) {
            return;
        }

        boolean scraping = true;
        int page = 0;
        while (scraping) {
            Document site = getPage(asset.getScraperId(), page);
            Element table = site.select("table.pd.leftcolumnwidth > tbody").first();
            Elements rows = table.select("tr.ps, tr.pl");

            LocalDate lastRowDate = LocalDate.parse((rows.get(rows.size() - 1)).child(0).text(),
                    DateTimeFormatter.ofPattern("d.M.yyyy"));
            if (lastRowDate.isAfter(maxDate)) {
                page++;
                continue;
            }

            for (Element row : rows) {
                LocalDate rowDate = LocalDate.parse(row.child(0).text(), DateTimeFormatter.ofPattern("d.M.yyyy"));

                if (rowDate.isBefore(minDate)) {
                    scraping = false;
                    break;
                }

                AssetPriceHistory assetPriceHistory = new AssetPriceHistory();

                switch (exchange.getName()) {
                case "BCPP":
                    if (BooleanUtils.isNotTrue(StringUtils.isBlank(row.child(1).text()))) {
                        assetPriceHistory
                                .setClosingPrice(new BigDecimal(StringUtils.deleteWhitespace(row.child(1).text())));
                        break;
                    } else {
                        continue;
                    }
                case "RMS":
                    if (BooleanUtils.isNotTrue(StringUtils.isBlank(row.child(5).text()))) {
                        assetPriceHistory
                                .setClosingPrice(new BigDecimal(StringUtils.deleteWhitespace(row.child(5).text())));
                        break;
                    } else {
                        continue;
                    }
                default:
                    throw new IllegalArgumentException("Scraping error...");
                }

                assetPriceHistory.setAsset(asset);
                assetPriceHistory.setDate(rowDate);
                assetPriceHistory.setExchange(exchange);

                assetPriceHistoryRepository.save(assetPriceHistory);

            }

            page++;

            Thread.sleep(new Random().nextInt(200) + 3000l);
        }

        log.debug("Scraping finished...");
    }

    private Document getPage(String assetId, int page) throws IOException {
        Map<String, Object> urlParametersMap = new HashMap<>();
        urlParametersMap.put("assetId", assetId);
        urlParametersMap.put("pageSize", PAGE_SIZE);
        urlParametersMap.put("firstRow", PAGE_SIZE * page);
        StringSubstitutor sub = new StringSubstitutor(urlParametersMap);
        String url = sub.replace(urlTemplate);
        log.trace("URL: {}", url);
        return Jsoup.connect(url).get();
    }

}
