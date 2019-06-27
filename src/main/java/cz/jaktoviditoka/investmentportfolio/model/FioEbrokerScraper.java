package cz.jaktoviditoka.investmentportfolio.model;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow.CellIterator;
import com.gargoylesoftware.htmlunit.util.Cookie;
import cz.jaktoviditoka.investmentportfolio.domain.FioEbrokerTransaction;
import cz.jaktoviditoka.investmentportfolio.domain.TransactionType;
import cz.jaktoviditoka.investmentportfolio.entity.*;
import cz.jaktoviditoka.investmentportfolio.repository.AppUserRepository;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.CookieCacheRepository;
import cz.jaktoviditoka.investmentportfolio.repository.LocationRepository;
import cz.jaktoviditoka.investmentportfolio.security.PasswordCryptoProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FioEbrokerScraper {

    @Autowired
    AppUserRepository userRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    CookieCacheRepository cookieCacheRepository;

    @Autowired
    ModelMapper modelMapper;
    
    @Autowired
    PasswordCryptoProvider passwordCryptoProvider;

    WebClient webClient = new WebClient();

    String loginUrl = "https://www.fio.cz/e-broker/e-broker.cgi";
    String transactionsUrl = "https://www.fio.cz/e-broker/e-obchody.cgi?obchody_DAT_od=${dateFrom}&obchody_DAT_do=${dateTo}";
    
    public void login(String username, String password)
            throws FailingHttpStatusCodeException, MalformedURLException, IOException {
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
        loginUserName.type(username);
        loginPassword.type(password);
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
    
    public List<Transaction> getTransactions(AppUser user) throws IOException {      
        return getTransactions(user.getFioEbrokerUsername(), passwordCryptoProvider.decrypt(user.getFioEbrokerPassword()), user);
    }

    public List<Transaction> getTransactions(String username, String password, AppUser user) throws IOException {       
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
                login(username, password);
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
                }
            }
            from = from.minusYears(1);
            to = to.minusYears(1);
        }

        List<Transaction> transactions = deposits(fioEbrokerTransactions);
        transactions.addAll(interests(fioEbrokerTransactions));
        transactions.addAll(specialFees(fioEbrokerTransactions));
        transactions.addAll(trades(fioEbrokerTransactions));
        transactions.stream().sorted((el1, el2) -> el1.getTimestamp().compareTo(el2.getTimestamp())).forEach(el -> el.setUser(user));
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
                        throw new IllegalArgumentException("Location not found.");
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
        return transactions.stream()
                .filter(el -> Objects.nonNull(el.getType()))
                .map(el -> {
                    TransactionPart transactionFrom = new TransactionPart();
                    TransactionPart transactionTo = new TransactionPart();

                    if (el.getComment().contains("Nákup")) {

                        Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                        if (currencyOpt.isPresent()) {
                            transactionFrom.setAsset(currencyOpt.get());
                            transactionFrom.setAmount(el.getAmount().multiply(el.getPrice()));
                        } else {
                            throw new IllegalArgumentException("Asset not found.");
                        }

                        Optional<Asset> feeAssetOpt = assetRepository.findByTicker(el.getFeeAsset());
                        if (feeAssetOpt.isPresent()) {
                            transactionFrom.setFeeAsset(feeAssetOpt.get());
                            transactionFrom.setFeeAmount(el.getFeeAmount());
                        } else {
                            throw new IllegalArgumentException("Asset not found.");
                        }

                        Optional<Asset> assetOpt = assetRepository.findByTicker(el.getAsset());
                        if (assetOpt.isPresent()) {
                            transactionTo.setAsset(assetOpt.get());
                            transactionTo.setAmount(el.getAmount());
                        } else {
                            throw new IllegalArgumentException("Asset not found.");
                        }

                    } else if (el.getComment().contains("Prodej")) {

                        Optional<Asset> assetOpt = assetRepository.findByTicker(el.getAsset());
                        if (assetOpt.isPresent()) {
                            transactionFrom.setAsset(assetOpt.get());
                            transactionFrom.setAmount(el.getAmount());
                        } else {
                            throw new IllegalArgumentException("Asset not found.");
                        }

                        Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                        if (currencyOpt.isPresent()) {
                            transactionTo.setAsset(currencyOpt.get());
                            transactionTo.setAmount(el.getAmount().multiply(el.getPrice()));
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

                    } else if (BooleanUtils.and(new Boolean[] {
                            el.getComment().contains("Čistá cena"),
                            el.getComment().contains("AUV") })) {

                        Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                        if (currencyOpt.isPresent()) {
                            transactionFrom.setAsset(currencyOpt.get());

                            Pattern pricePattern = Pattern.compile("(?<=Čistá cena: )[0-9]*.[0-9]*");
                            Matcher priceMatcher = pricePattern.matcher(el.getComment());
                            if (priceMatcher.find()) {
                                transactionFrom
                                        .setAmount(el.getAmount().multiply(new BigDecimal(priceMatcher.group()).divide(BigDecimal.valueOf(100))));
                            } else {
                                throw new IllegalArgumentException("Failed to parse bond trade.");
                            }

                        } else {
                            throw new IllegalArgumentException("Asset not found.");
                        }

                        Optional<Asset> feeAssetOpt = assetRepository.findByTicker(el.getFeeAsset());
                        if (feeAssetOpt.isPresent()) {
                            transactionFrom.setFeeAsset(feeAssetOpt.get());

                            Pattern feePattern = Pattern.compile("(?<=AUV: )[0-9]*.[0-9]*");
                            Matcher feeMatcher = feePattern.matcher(el.getComment());
                            if (feeMatcher.find()) {
                                transactionFrom.setFeeAmount(el.getFeeAmount().add(new BigDecimal(feeMatcher.group())));
                            } else {
                                throw new IllegalArgumentException("Failed to parse bond trade.");
                            }
                        } else {
                            throw new IllegalArgumentException("Asset not found.");
                        }

                        Optional<Asset> assetOpt = assetRepository.findByTicker(el.getAsset());
                        if (assetOpt.isPresent()) {
                            transactionTo.setAsset(assetOpt.get());
                            transactionTo.setAmount(el.getAmount());
                        } else {
                            throw new IllegalArgumentException("Asset not found.");
                        }

                    } else {
                        throw new IllegalArgumentException("Unknown trade type.");
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
                            .type(TransactionType.TRADE)
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

                    TransactionPart transactionTo = null;
                    if (Objects.nonNull(el.getAsset())) {
                        transactionTo = new TransactionPart();
                        Optional<Asset> assetOpt = assetRepository.findByTicker(el.getAsset());
                        if (assetOpt.isPresent()) {
                            transactionTo.setAsset(assetOpt.get());
                            transactionTo.setAmount(BigDecimal.ZERO);
                        } else {
                            throw new IllegalArgumentException("Asset not found.");
                        }
                    }

                    TransactionPart transactionFrom = new TransactionPart();

                    Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                    if (currencyOpt.isPresent()) {
                        transactionFrom.setAsset(currencyOpt.get());
                        transactionFrom.setAmount(el.getTotalAmount().abs());
                    } else {
                        throw new IllegalArgumentException("Asset not found.");
                    }

                    Optional<Location> locationOpt = locationRepository.findByName("Fio e-Broker");
                    if (locationOpt.isPresent()) {
                        if (Objects.nonNull(transactionTo)) {
                            transactionTo.setLocation(locationOpt.get());
                        }
                        transactionFrom.setLocation(locationOpt.get());
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
