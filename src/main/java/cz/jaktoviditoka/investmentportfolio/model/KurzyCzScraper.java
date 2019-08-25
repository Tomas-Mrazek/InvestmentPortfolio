package cz.jaktoviditoka.investmentportfolio.model;

import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.AssetPrice;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.repository.AssetPriceRepository;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KurzyCzScraper {

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetPriceRepository assetPriceRepository;

    private static final String URL_TEMPLATE = "https://akcie-cz.kurzy.cz/prehled.asp?T=PK&CP=${assetId}&MAXROWS=1&Day=${date}";
    private static final String PRICE_ASSET = "CZK";

    @Transactional
    public void scrape(Asset asset, Exchange exchange, LocalDate scrapeDate) throws IOException, InterruptedException {
        log.trace("Scraping...");

        Asset priceAsset = assetRepository.findByName(PRICE_ASSET)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        List<LocalDate> existingDates = assetPriceRepository.findByAssetAndExchange(asset, exchange).stream()
                .map(mapper -> mapper.getDate())
                .collect(Collectors.toList());

        List<LocalDate> missingDates = scrapeDate.datesUntil(LocalDate.now())
                .filter(el -> BooleanUtils.isNotTrue(existingDates.contains(el)))
                .collect(Collectors.toList());

        Integer columnPrice;
        switch (exchange.getAbbreviation()) {
            case BCPP:
                columnPrice = 1;
                break;
            case RMS:
                columnPrice = 5;
                break;
            default:
                throw new IllegalArgumentException("Incompatible exchange.");
        }

        for (LocalDate date : missingDates) {
            log.debug("Scraping date: {}", date);

            Document site = Jsoup
                    .connect(getPageUrl(asset.getScraperId(), date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))))
                    .get();
            Element table = site.select("table.pd.leftcolumnwidth > tbody").first();
            Elements rows = table.select("tr.ps, tr.pl");
            Element row = rows.first();

            BigDecimal price;
            if (BooleanUtils.isNotTrue(StringUtils.isBlank(row.child(columnPrice).text()))) {
                price = new BigDecimal(StringUtils.deleteWhitespace(row.child(columnPrice).text()));
            } else {
                price = assetPriceRepository.findByAssetAndExchange(asset, exchange).stream()
                        .filter(el -> el.getDate().isBefore(date))
                        .max((el1, el2) -> el1.getDate().compareTo(el2.getDate()))
                        .orElseThrow(() -> new IllegalArgumentException("Unknown price."))
                        .getPrice();
            }

            AssetPrice assetPrice = AssetPrice.builder()
                    .date(date)
                    .asset(asset)
                    .price(price)
                    .priceAsset(priceAsset)
                    .exchange(exchange)
                    .build();
            assetPriceRepository.save(assetPrice);

            Thread.sleep(new Random().nextInt(20) + 200l);
        }

        log.trace("Scraping finished...");
    }

    private String getPageUrl(String assetId, String date) {
        Map<String, Object> urlParametersMap = new HashMap<>();
        urlParametersMap.put("assetId", assetId);
        urlParametersMap.put("date", date);
        StringSubstitutor sub = new StringSubstitutor(urlParametersMap);
        return sub.replace(URL_TEMPLATE);
    }

}
