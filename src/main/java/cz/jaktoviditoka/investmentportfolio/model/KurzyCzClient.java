package cz.jaktoviditoka.investmentportfolio.model;

import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class KurzyCzClient {

    private static final String URI = "uri";
    private static final String SLUG = "slug";
    private static final String TYPE = "typtrhubcp";
    private static final String ISIN = "ISIN";
    private static final String BIC = "BIC";
    private static final String NAME = "JmenoB";
    private static final String TRADING_DAY = "obchodniden";
    private static final String PRICE_CLOSE = "KurzB";
    private static final String PRICE_CHANGE = "ZmenaB";
    private static final String PRICE_OPEN = "OpenCena";
    private static final String PRICE_MIN = "MinCena";
    private static final String PRICE_MAX = "MaxCena";
    private static final String PRICE_MIN_YEAR = "Min52B";
    private static final String PRICE_MAX_YEAR = "Max52B";
    private static final String TRADE_SHARES = "PocetB";
    private static final String TRADE_VOLUME = "ObjemB";

    private static final String URL_TEMPLATE = "https://akcie-cz.kurzy.cz/${exchange}/kurzy_${date}/${exchange}.csv";

    @Autowired
    RestTemplate restTemplate;

    @Transactional
    public KurzyCzPrice importPrice(Exchange exchange, LocalDate date) throws IOException {
        log.debug("Importing CSV – {}", date.format(DateTimeFormatter.ofPattern("d.M.yyyy")));

        List<KurzyCzPriceDay> listOfPricesDay = new ArrayList<>();

        String url;
        switch (exchange.getAbbreviation()) {
        case BCPP:
            url = getPageUrl("burza", date.format(DateTimeFormatter.ofPattern("d.M.yyyy")));
            break;
        case RMS:
            url = getPageUrl("rm-system", date.format(DateTimeFormatter.ofPattern("d.M.yyyy")));
            break;
        default:
            throw new IllegalArgumentException("Incompatible exchange.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> csv = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        CSVParser records = CSVFormat.newFormat(';')
                .withFirstRecordAsHeader()
                .withIgnoreEmptyLines()
                .withTrim()
                .parse(new StringReader(csv.getBody()));

        for (CSVRecord record : records) {
            KurzyCzPriceDay priceDay = KurzyCzPriceDay.builder()
                    .uri(record.get(URI))
                    .slug(record.get(SLUG))
                    .type(record.get(TYPE))
                    .isin(record.get(ISIN))
                    .bic(record.get(BIC))
                    .name(record.get(NAME))
                    .tradingDay(record.get(TRADING_DAY))
                    .priceClose(record.get(PRICE_CLOSE))
                    .priceChange(record.get(PRICE_CHANGE))
                    .priceOpen(record.get(PRICE_OPEN))
                    .priceMinDay(record.get(PRICE_MIN))
                    .priceMaxDay(record.get(PRICE_MAX))
                    .priceMinYear(record.get(PRICE_MIN_YEAR))
                    .priceMaxYear(record.get(PRICE_MAX_YEAR))
                    .tradeShares(record.get(TRADE_SHARES))
                    .tradeVolume(record.get(TRADE_VOLUME))
                    .build();

            listOfPricesDay.add(priceDay);
        }

        return KurzyCzPrice.builder()
                .date(date.format(DateTimeFormatter.ISO_DATE))
                .listOfPrices(listOfPricesDay)
                .build();
    }

    private String getPageUrl(String exchange, String date) {
        Map<String, Object> urlParametersMap = new HashMap<>();
        urlParametersMap.put("exchange", exchange);
        urlParametersMap.put("date", date);
        StringSubstitutor sub = new StringSubstitutor(urlParametersMap);
        return sub.replace(URL_TEMPLATE);
    }

}
