package cz.jaktoviditoka.investmentscraper.model;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow.CellIterator;
import com.gargoylesoftware.htmlunit.util.Cookie;
import cz.jaktoviditoka.investmentscraper.domain.FioEbrokerTransaction;
import cz.jaktoviditoka.investmentscraper.domain.TransactionType;
import cz.jaktoviditoka.investmentscraper.entity.*;
import cz.jaktoviditoka.investmentscraper.repository.AssetRepository;
import cz.jaktoviditoka.investmentscraper.repository.CookieCacheRepository;
import cz.jaktoviditoka.investmentscraper.repository.LocationRepository;
import cz.jaktoviditoka.investmentscraper.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FioEbrokerScraper {

    @Autowired
    UserRepository userRepository;
    
    @Autowired
    AssetRepository assetRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    CookieCacheRepository cookieCacheRepository;

    @Autowired
    ModelMapper modelMapper;

    WebClient webClient = new WebClient();

    String loginUrl = "https://www.fio.cz/e-broker/e-broker.cgi";
    String transactionsUrl = "https://www.fio.cz/e-broker/e-obchody.cgi?obchody_DAT_od=${dateFrom}&obchody_DAT_do=${dateTo}";

    @EventListener(ApplicationReadyEvent.class)
    public void login() throws FailingHttpStatusCodeException, MalformedURLException, IOException {

        Optional<User> userOpt = userRepository.findByName("Tomáš Mrázek");
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        
        List<CookieCache> cookies = cookieCacheRepository.findAll();
        if (BooleanUtils.isNotTrue(cookies.isEmpty())) {
            boolean logged = cookies.stream()
                    .map(el -> el.getCreatedAt())
                    .filter(el -> Objects.nonNull(el))
                    .allMatch(el -> el.plusMinutes(15).isAfter(LocalDateTime.now()));
            if (logged) {
                log.debug("Fio e-Broker already logged.");
                return;
            } else {
                cookieCacheRepository.deleteAll();
            }
        }
        
        HtmlPage loginPage = webClient.getPage(loginUrl);
        HtmlTextInput loginUserName = loginPage.getElementByName("LOGIN_USERNAME");
        HtmlPasswordInput loginPassword = loginPage.getElementByName("LOGIN_PASSWORD");
        HtmlSubmitInput submit = loginPage.getElementByName("SUBMIT");
        loginUserName.type(userOpt.get().getFioEbrokerUsername());
        loginPassword.type(userOpt.get().getFioEbrokerPassword());
        HtmlPage broker = submit.click();

        if (BooleanUtils.isNotTrue(broker.getUrl().toString().contains("/e-portfolio.cgi"))) {
            webClient.close();
            throw new RuntimeException("Login to e-Broker failed.");
        } else {
            webClient.getCookieManager().getCookies().stream()
                    .map(el -> modelMapper.map(el, CookieCache.class))
                    .forEach(el -> {
                        el.setCreatedAt(LocalDateTime.now());
                        cookieCacheRepository.save(el);
                    });
            log.debug("Fio e-Broker logged.");
        }
    }

    public List<Transaction> getTransactions() throws IOException {
        cookieCacheRepository.findAll().stream()
                .map(el -> new Cookie(
                        el.getDomain(),
                        el.getName(),
                        el.getValue(),
                        el.getPath(),
                        el.getExpires(),
                        el.isSecure(),
                        el.isHttponly()))
                .forEach(el -> webClient.getCookieManager().addCookie(el));

        List<FioEbrokerTransaction> fioEbrokerTransactions = new ArrayList<>();

        boolean scraping = true;

        LocalDate from = LocalDate.now().minusYears(1);
        LocalDate to = LocalDate.now();

        while (scraping) {
            HtmlPage transactionPage = getTransactionPage(from, to);
            if (transactionPage.asXml().contains("login_table")) {
                cookieCacheRepository.deleteAll();
                login();
                transactionPage = getTransactionPage(from, to);
            }
            HtmlTable table = transactionPage.getHtmlElementById("obchody_full_table");
            for (HtmlTableBody body : table.getBodies()) {
                List<HtmlTableRow> rows = body.getByXPath(".//tr[@class='e' or @class='o']");

                if (rows.size() <= 1) {
                    scraping = false;
                    break;
                }
                for (HtmlTableRow row : rows) {
                    if (row.equals(rows.get(rows.size() - 1))) {
                        continue;
                    }
                    FioEbrokerTransaction transaction = createFioEbrokerTransaction(row);
                    fioEbrokerTransactions.add(transaction);
                    // log.debug("FioEbrokerTransaction: {}", transaction);
                }
            }
            from = from.minusYears(1);
            to = to.minusYears(1);
        }

        List<Transaction> transactions = deposits(fioEbrokerTransactions);
        transactions.addAll(interests(fioEbrokerTransactions));
        transactions.addAll(specialFees(fioEbrokerTransactions));

        return transactions;
    }

    private HtmlPage getTransactionPage(LocalDate from, LocalDate to)
            throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        Map<String, Object> urlParametersMap = new HashMap<>();
        urlParametersMap.put("dateFrom", from.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        urlParametersMap.put("dateTo", to.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        StringSubstitutor sub = new StringSubstitutor(urlParametersMap);
        String url = sub.replace(transactionsUrl);
        return webClient.getPage(url);
    }

    private FioEbrokerTransaction createFioEbrokerTransaction(HtmlTableRow row) {
        CellIterator iterator = row.getCellIterator();

        FioEbrokerTransaction transaction = new FioEbrokerTransaction();
        transaction.setTimestamp(LocalDateTime.parse(iterator.nextCell().asText(),
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));

        DomAttr type = iterator.nextCell().getFirstByXPath(".//img/@title");
        if (Objects.nonNull(type)) {
            transaction.setType(type.getValue());
        }

        String asset = iterator.nextCell().asText();
        if (BooleanUtils.isNotTrue(asset.isBlank())) {
            transaction.setAsset(asset);
        }

        // Price
        String priceString = StringUtils.deleteWhitespace(iterator.nextCell().asText().replace(",", "."));
        if (BooleanUtils.isNotTrue(priceString.isBlank())) {
            BigDecimal price = new BigDecimal(priceString);
            if (price.compareTo(BigDecimal.ZERO) != 0) {
                transaction.setPrice(price);
            }
        }

        // Amount
        String amountString = StringUtils.deleteWhitespace(iterator.nextCell().asText().replace(",", "."));
        if (BooleanUtils.isNotTrue(amountString.isBlank())) {
            BigDecimal amount = new BigDecimal(amountString);
            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                transaction.setAmount(amount);
            }
        }

        // Currency
        String currency = iterator.nextCell().asText();
        if (BooleanUtils.isNotTrue(currency.isBlank())) {
            transaction.setCurrency(currency);
        }

        String totalPriceInCzkStr = StringUtils.deleteWhitespace(iterator.nextCell().asText().replace(",", "."));
        String feeInCzkStr = StringUtils.deleteWhitespace(iterator.nextCell().asText().replace(",", "."));
        String totalPriceInUsdStr = StringUtils.deleteWhitespace(iterator.nextCell().asText().replace(",", "."));
        String feeInUsdStr = StringUtils.deleteWhitespace(iterator.nextCell().asText().replace(",", "."));
        String totalPriceInEurStr = StringUtils.deleteWhitespace(iterator.nextCell().asText().replace(",", "."));
        String feeInEurStr = StringUtils.deleteWhitespace(iterator.nextCell().asText().replace(",", "."));

        if (List.of(totalPriceInCzkStr, totalPriceInUsdStr, totalPriceInEurStr).stream()
                .filter(el -> StringUtils.isNotBlank(el))
                .count() != 1) {
            throw new IllegalArgumentException("Parsing error...");
        }

        // Total price
        BigDecimal totalAmount;
        if (BooleanUtils.isNotTrue(totalPriceInCzkStr.isBlank())) {
            totalAmount = new BigDecimal(totalPriceInCzkStr);
            transaction.setTotalAmountAsset("CZK");
        } else if (BooleanUtils.isNotTrue(totalPriceInUsdStr.isBlank())) {
            totalAmount = new BigDecimal(totalPriceInUsdStr);
            transaction.setTotalAmountAsset("USD");
        } else if (BooleanUtils.isNotTrue(totalPriceInEurStr.isBlank())) {
            totalAmount = new BigDecimal(totalPriceInEurStr);
            transaction.setTotalAmountAsset("EUR");
        } else {
            throw new IllegalArgumentException("Parsing error...");
        }
        transaction.setTotalAmount(totalAmount);

        // Fee
        BigDecimal feeAmount;
        if (BooleanUtils.isNotTrue(feeInCzkStr.isBlank())) {
            feeAmount = new BigDecimal(feeInCzkStr);
            transaction.setFeeAsset("CZK");
        } else if (BooleanUtils.isNotTrue(feeInUsdStr.isBlank())) {
            feeAmount = new BigDecimal(feeInUsdStr);
            transaction.setFeeAsset("USD");
        } else if (BooleanUtils.isNotTrue(feeInEurStr.isBlank())) {
            feeAmount = new BigDecimal(feeInEurStr);
            transaction.setFeeAsset("EUR");
        } else {
            throw new IllegalArgumentException("Parsing error...");
        }
        transaction.setFeeAmount(feeAmount);

        transaction.setComment(iterator.nextCell().asText());

        return transaction;
    }

    private List<Transaction> deposits(List<FioEbrokerTransaction> transactions) {
        return transactions.stream()
                .filter(el -> el.getComment().contains("Vloženo na účet"))
                .filter(el -> BooleanUtils.isNotTrue(el.getComment().contains("MONETA MONEY BANK")))
                .map(el -> {
                    TransactionPart transactionTo = new TransactionPart();

                    Optional<Asset> assetOpt = assetRepository.findByTicker(el.getCurrency());
                    if (assetOpt.isPresent()) {
                        transactionTo.setAsset(assetOpt.get());
                        transactionTo.setAmount(el.getTotalAmount());
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    Optional<Asset> feeAssetOpt = assetRepository.findByTicker(el.getFeeAsset());
                    if (feeAssetOpt.isPresent()) {
                        transactionTo.setFeeAsset(feeAssetOpt.get());
                        transactionTo.setFeeAmount(el.getFeeAmount());
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    Optional<Location> locationOpt = locationRepository.findByName("Fio e-Broker");
                    if (locationOpt.isPresent()) {
                        transactionTo.setLocation(locationOpt.get());
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    return Transaction.builder()
                            .timestamp(el.getTimestamp())
                            .type(TransactionType.DEPOSIT)
                            .to(transactionTo)
                            .comment(el.getComment())
                            .build();

                })
                .collect(Collectors.toList());
    }

    private List<Transaction> trades(List<FioEbrokerTransaction> transactions) {
        transactions.stream()
                .filter(el -> Objects.nonNull(el.getType())).map(el -> {
                    TransactionPart transactionFrom = new TransactionPart();

                    if (el.getComment().contains("Nákup")) {
                        
                    } else if (el.getComment().contains("Prodej")) {
                        
                    } else {
                        throw new IllegalArgumentException("Unknown trade type.");
                    }
                    
                    
                    Optional<Asset> assetOpt = assetRepository.findByTicker(el.getAsset());
                    if (assetOpt.isPresent()) {
                        transactionFrom.setAsset(assetOpt.get());
                        transactionFrom.setAmount(el.getAmount().abs());
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    TransactionPart transactionTo = new TransactionPart();

                    Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                    if (currencyOpt.isPresent()) {
                        transactionTo.setAsset(currencyOpt.get());
                        transactionTo.setAmount(el.getAmount());
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    Optional<Location> locationOpt = locationRepository.findByName("Fio e-Broker");
                    if (locationOpt.isPresent()) {
                        transactionFrom.setLocation(locationOpt.get());
                        transactionTo.setLocation(locationOpt.get());
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    return Transaction.builder()
                            .timestamp(el.getTimestamp())
                            .type(TransactionType.INTEREST)
                            .from(transactionFrom)
                            .to(transactionTo)
                            .comment(el.getComment())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<Transaction> interests(List<FioEbrokerTransaction> transactions) {
        return transactions.stream()
                .filter(el -> BooleanUtils.or(new Boolean[] {
                        BooleanUtils.and(new Boolean[] {
                                el.getComment().contains("Vloženo na účet"),
                                el.getComment().contains("MONETA MONEY BANK") }),
                        Boolean.valueOf((el.getComment().contains(el.getAsset() + " - Dividenda"))) }))
                .map(el -> {
                    TransactionPart transactionFrom = new TransactionPart();

                    if (el.getComment().contains("MONETA MONEY BANK")) {
                        el.setAsset("BAAGECBA");
                        el.setAmount(el.getTotalAmount());
                    }
                    Optional<Asset> assetOpt = assetRepository.findByTicker(el.getAsset());
                    if (assetOpt.isPresent()) {
                        transactionFrom.setAsset(assetOpt.get());
                        transactionFrom.setAmount(BigDecimal.ZERO);
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    TransactionPart transactionTo = new TransactionPart();

                    Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                    if (currencyOpt.isPresent()) {
                        transactionTo.setAsset(currencyOpt.get());
                        transactionTo.setAmount(el.getAmount());
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    Optional<Location> locationOpt = locationRepository.findByName("Fio e-Broker");
                    if (locationOpt.isPresent()) {
                        transactionFrom.setLocation(locationOpt.get());
                        transactionTo.setLocation(locationOpt.get());
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    return Transaction.builder()
                            .timestamp(el.getTimestamp())
                            .type(TransactionType.INTEREST)
                            .from(transactionFrom)
                            .to(transactionTo)
                            .comment(el.getComment())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<Transaction> specialFees(List<FioEbrokerTransaction> transactions) {
        return transactions.stream()
                .filter(el -> BooleanUtils.or(new Boolean[] {
                        el.getComment().contains("Daň z divid."),
                        el.getComment().contains("Poplatek za on-line data"),
                        el.getComment().contains("Poplatek za připsání dividend"),
                }))
                .map(el -> {

                    TransactionPart transactionFrom = null;
                    if (Objects.nonNull(el.getAsset())) {
                        transactionFrom = new TransactionPart();
                        Optional<Asset> assetOpt = assetRepository.findByTicker(el.getAsset());
                        if (assetOpt.isPresent()) {
                            transactionFrom.setAsset(assetOpt.get());
                            transactionFrom.setAmount(BigDecimal.ZERO);
                        } else {
                            throw new IllegalArgumentException("Asset not found.");
                        }
                    }

                    TransactionPart transactionTo = new TransactionPart();

                    Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                    if (currencyOpt.isPresent()) {
                        transactionTo.setAsset(currencyOpt.get());
                        transactionTo.setAmount(el.getTotalAmount().abs());
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    Optional<Location> locationOpt = locationRepository.findByName("Fio e-Broker");
                    if (locationOpt.isPresent()) {
                        if (Objects.nonNull(transactionFrom)) {
                            transactionFrom.setLocation(locationOpt.get());
                        }
                        transactionTo.setLocation(locationOpt.get());
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    return Transaction.builder()
                            .timestamp(el.getTimestamp())
                            .type(TransactionType.SPECIAL_FEE)
                            .from(transactionFrom)
                            .to(transactionTo)
                            .comment(el.getComment())
                            .build();
                })
                .collect(Collectors.toList());

    }

}
