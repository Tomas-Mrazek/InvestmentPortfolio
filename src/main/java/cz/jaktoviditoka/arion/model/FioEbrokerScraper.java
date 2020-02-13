package cz.jaktoviditoka.arion.model;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow.CellIterator;
import com.gargoylesoftware.htmlunit.util.Cookie;
import cz.jaktoviditoka.arion.domain.AssetType;
import cz.jaktoviditoka.arion.domain.ExchangeAbbrEnum;
import cz.jaktoviditoka.arion.domain.FioEbrokerTransaction;
import cz.jaktoviditoka.arion.domain.TransactionType;
import cz.jaktoviditoka.arion.entity.*;
import cz.jaktoviditoka.arion.repository.AppUserRepository;
import cz.jaktoviditoka.arion.repository.AssetRepository;
import cz.jaktoviditoka.arion.repository.CookieCacheRepository;
import cz.jaktoviditoka.arion.repository.ExchangeRepository;
import cz.jaktoviditoka.arion.security.PasswordCryptoProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    ExchangeRepository exchangeRepository;

    @Autowired
    CookieCacheRepository cookieCacheRepository;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    PasswordCryptoProvider passwordCryptoProvider;

    WebClient webClient = new WebClient();

    private static final String LOGIN_URL = "https://www.fio.cz/e-broker/e-broker.cgi";
    private static final String TRANSACTION_URL = "https://www.fio.cz/e-broker/e-obchody.cgi?obchody_DAT_od=${dateFrom}&obchody_DAT_do=${dateTo}&PEN_zeme=&ID_trh=";

    private static final ExchangeAbbrEnum DEFAULT_CURRENCY_EXCHANGE = ExchangeAbbrEnum.FIO;
    private static final ExchangeAbbrEnum DEFAULT_CZECH_STOCK_EXCHANGE = ExchangeAbbrEnum.BCPP;
    private static final ExchangeAbbrEnum DEFAULT_FOREIGN_STOCK_EXCHANGE = ExchangeAbbrEnum.NYSE;
    private static final String DEFAULT_LOCATION_NAME = "Fio e-Broker";

    private static final DateTimeFormatter PAGE_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final ZoneId TIMEZONE = ZoneId.of("Europe/Prague");

    private static final String EXCEPTION_MESSAGE_ASSET_NOT_FOUND = "Asset not found.";
    private static final String EXCEPTION_MESSAGE_PARSING_ERROR = "Parsing error...";

    Exchange defaultCurrencyExchange;
    Exchange defaultCzechStockExchange;
    Exchange defaultForeignStockExchange;

    private void login(String username, String password) throws IOException {
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

        HtmlPage loginPage = webClient.getPage(LOGIN_URL);
        HtmlTextInput loginUserName = loginPage.getElementByName("LOGIN_USERNAME");
        HtmlPasswordInput loginPassword = loginPage.getElementByName("LOGIN_PASSWORD");
        HtmlSubmitInput submit = loginPage.getElementByName("SUBMIT");
        loginUserName.type(username);
        loginPassword.type(password);
        HtmlPage broker = submit.click();

        if (BooleanUtils.isNotTrue(broker.getUrl().toString().contains("/e-portfolio.cgi"))) {
            webClient.close();
            throw new IllegalArgumentException("Login to e-Broker failed.");
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
        return getTransactions(user.getFioEbrokerUsername(),
                passwordCryptoProvider.decrypt(user.getFioEbrokerPassword()), user);
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

        defaultCurrencyExchange = exchangeRepository.findByAbbreviation(DEFAULT_CURRENCY_EXCHANGE).orElseThrow();
        defaultCzechStockExchange = exchangeRepository.findByAbbreviation(DEFAULT_CZECH_STOCK_EXCHANGE).orElseThrow();
        defaultForeignStockExchange = exchangeRepository.findByAbbreviation(DEFAULT_FOREIGN_STOCK_EXCHANGE)
                .orElseThrow();

        LocalDate from = LocalDate.now().minusYears(1);
        LocalDate to = LocalDate.now();

        boolean scraping = true;

        while (scraping) {
            HtmlPage transactionPage = getTransactionPage(
                    from.format(PAGE_DATE_TIME_FORMAT),
                    to.format(PAGE_DATE_TIME_FORMAT));
            if (transactionPage.asXml().contains("login_table")) {
                cookieCacheRepository.deleteAll();
                login(username, password);
                transactionPage = getTransactionPage(
                        from.format(PAGE_DATE_TIME_FORMAT),
                        to.format(PAGE_DATE_TIME_FORMAT));
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

        getTransactionPage(StringUtils.EMPTY, StringUtils.EMPTY);

        fioEbrokerTransactions.stream().forEach(el -> log.debug("{}", el));

        List<Transaction> transactions = new ArrayList<>();
        transactions.addAll(deposits(fioEbrokerTransactions));
        transactions.addAll(interests(fioEbrokerTransactions));
        transactions.addAll(specialFees(fioEbrokerTransactions));
        transactions.addAll(trades(fioEbrokerTransactions));
        transactions.stream().forEach(el -> el.setUser(user));
        return transactions;
    }

    private HtmlPage getTransactionPage(String from, String to) throws IOException {
        Map<String, Object> urlParametersMap = new HashMap<>();
        urlParametersMap.put("dateFrom", from);
        urlParametersMap.put("dateTo", to);
        StringSubstitutor sub = new StringSubstitutor(urlParametersMap);
        String url = sub.replace(TRANSACTION_URL);
        return webClient.getPage(url);
    }

    private FioEbrokerTransaction createFioEbrokerTransaction(HtmlTableRow row) {
        CellIterator iterator = row.getCellIterator();

        FioEbrokerTransaction transaction = new FioEbrokerTransaction();
        transaction.setTimestamp(ZonedDateTime.of(
                LocalDateTime.parse(iterator.nextCell().asText(),
                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                TIMEZONE));

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
            throw new IllegalArgumentException();
        }

        // Total price
        BigDecimal totalAmount;
        if (BooleanUtils.isNotTrue(totalPriceInCzkStr.isBlank())) {
            totalAmount = new BigDecimal(totalPriceInCzkStr);
            transaction.setTotalAmountCurrency("CZK");
        } else if (BooleanUtils.isNotTrue(totalPriceInUsdStr.isBlank())) {
            totalAmount = new BigDecimal(totalPriceInUsdStr);
            transaction.setTotalAmountCurrency("USD");
        } else if (BooleanUtils.isNotTrue(totalPriceInEurStr.isBlank())) {
            totalAmount = new BigDecimal(totalPriceInEurStr);
            transaction.setTotalAmountCurrency("EUR");
        } else {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE_PARSING_ERROR);
        }
        transaction.setTotalAmount(totalAmount);

        // Fee
        BigDecimal feeAmount;
        if (BooleanUtils.isNotTrue(feeInCzkStr.isBlank())) {
            feeAmount = new BigDecimal(feeInCzkStr);
            transaction.setFeeCurrency("CZK");
        } else if (BooleanUtils.isNotTrue(feeInUsdStr.isBlank())) {
            feeAmount = new BigDecimal(feeInUsdStr);
            transaction.setFeeCurrency("USD");
        } else if (BooleanUtils.isNotTrue(feeInEurStr.isBlank())) {
            feeAmount = new BigDecimal(feeInEurStr);
            transaction.setFeeCurrency("EUR");
        } else {
            throw new IllegalArgumentException(EXCEPTION_MESSAGE_PARSING_ERROR);
        }
        transaction.setFeeAmount(feeAmount);

        transaction.setComment(iterator.nextCell().asText());

        return transaction;
    }

    private List<Transaction> deposits(List<FioEbrokerTransaction> transactions) {
        return transactions.stream()
                .filter(el -> el.getComment().contains("Vloženo na účet"))
                .filter(el -> BooleanUtils.isNotTrue(el.getComment().contains("27-9078470257/0100")))
                .map(el -> {
                    TransactionMovement transactionAdd = new TransactionMovement();
                    
                    Optional<Asset> assetOpt = assetRepository.findByTicker(el.getCurrency());
                    if (assetOpt.isPresent()) {
                        transactionAdd.setAsset(assetOpt.get());
                        transactionAdd.setAmount(el.getTotalAmount());
                    } else {
                        throw new IllegalArgumentException();
                    }

                    Optional<Asset> feeCurrencyOpt = assetRepository.findByTicker(el.getFeeCurrency());
                    if (feeCurrencyOpt.isPresent()) {
                        transactionAdd.setFeeAsset(feeCurrencyOpt.get());
                        transactionAdd.setFeeAmount(el.getFeeAmount());
                    } else {
                        throw new IllegalArgumentException(EXCEPTION_MESSAGE_ASSET_NOT_FOUND);
                    }

                    transactionAdd.setLocation(DEFAULT_LOCATION_NAME);

                    transactionAdd.setExchange(defaultCurrencyExchange);

                    return Transaction.builder()
                            .type(TransactionType.DEPOSIT)
                            .timestamp(el.getTimestamp())
                            .in(transactionAdd)
                            .comment(el.getComment())
                            .imported(true)
                            .build();

                })
                .collect(Collectors.toList());
    }

    // TODO missing withdrawal – not enough data

    private List<Transaction> trades(List<FioEbrokerTransaction> transactions) {
        return transactions.stream()
                .filter(el -> Objects.nonNull(el.getType()))
                .map(el -> {

                    TransactionMovement transactionRemove = new TransactionMovement();
                    TransactionMovement transactionAdd = new TransactionMovement();
                    
                    if (el.getComment().contains("Nákup")) {

                        Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                        if (currencyOpt.isPresent()) {
                            transactionRemove.setAsset(currencyOpt.get());
                            transactionRemove.setAmount(el.getAmount().multiply(el.getPrice()));
                        } else {
                            log.error("Asset not found – {}", el.getCurrency());
                            throw new IllegalArgumentException(EXCEPTION_MESSAGE_ASSET_NOT_FOUND);
                        }

                        Optional<Asset> feeCurrencyOpt = assetRepository.findByTicker(el.getFeeCurrency());
                        if (feeCurrencyOpt.isPresent()) {
                            transactionRemove.setFeeAsset(feeCurrencyOpt.get());
                            transactionRemove.setFeeAmount(el.getFeeAmount());
                        } else {
                            log.error("Asset not found – {}", el.getFeeCurrency());
                            throw new IllegalArgumentException(EXCEPTION_MESSAGE_ASSET_NOT_FOUND);
                        }

                        Optional<Asset> assetOpt = assetRepository.findByTicker(el.getAsset());
                        Asset asset;
                        if (assetOpt.isEmpty()) {
                            log.warn("Asset not found, creating placeholder – {}", el.getAsset());
                            asset = Asset.builder()
                                    .name(el.getAsset())
                                    .ticker(el.getAsset())
                                    .type(AssetType.STOCK)
                                    .build();
                            assetRepository.save(asset);
                        } else {
                            asset = assetOpt.get();
                        }

                        transactionAdd.setAsset(asset);
                        transactionAdd.setAmount(el.getAmount());

                        if (el.getType().contains("Převod mezi měnami")) {
                            transactionAdd.setExchange(defaultCurrencyExchange);
                            transactionRemove.setExchange(defaultCurrencyExchange);
                        }

                    } else if (el.getComment().contains("Prodej")) {

                        Optional<Asset> assetOpt = assetRepository.findByTicker(el.getAsset());
                        Asset asset;
                        if (assetOpt.isEmpty()) {
                            log.warn("Asset not found, creating placeholder – {}", el.getAsset());
                            asset = Asset.builder()
                                    .name(el.getAsset())
                                    .ticker(el.getAsset())
                                    .type(AssetType.STOCK)
                                    .build();
                            assetRepository.save(asset);
                        } else {
                            asset = assetOpt.get();
                        }
                        transactionRemove.setAsset(asset);
                        transactionRemove.setAmount(el.getAmount());

                        Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                        if (currencyOpt.isPresent()) {
                            transactionAdd.setAsset(currencyOpt.get());
                            transactionAdd.setAmount(el.getAmount().multiply(el.getPrice()));
                        } else {
                            log.error("Asset not found – {}", el.getCurrency());
                            throw new IllegalArgumentException(EXCEPTION_MESSAGE_ASSET_NOT_FOUND);
                        }

                        Optional<Asset> feeCurrencyOpt = assetRepository.findByTicker(el.getFeeCurrency());
                        if (feeCurrencyOpt.isPresent()) {
                            transactionAdd.setFeeAsset(feeCurrencyOpt.get());
                            transactionAdd.setFeeAmount(el.getFeeAmount());
                        } else {
                            log.error("Asset not found – {}", el.getFeeCurrency());
                            throw new IllegalArgumentException(EXCEPTION_MESSAGE_ASSET_NOT_FOUND);
                        }

                    } else if (el.getComment().contains("Čistá cena")
                            && el.getComment().contains("AUV")) {

                        Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                        if (currencyOpt.isPresent()) {
                            transactionRemove.setAsset(currencyOpt.get());

                            Pattern pricePattern = Pattern.compile("(?<=Čistá cena: )[0-9]*.[0-9]*");
                            Matcher priceMatcher = pricePattern.matcher(el.getComment());
                            if (priceMatcher.find()) {
                                transactionRemove.setAmount(el.getAmount().multiply(
                                        new BigDecimal(priceMatcher.group()).divide(BigDecimal.valueOf(100))));
                            } else {
                                throw new IllegalArgumentException("Failed to parse bond trade.");
                            }
                        } else {
                            log.error("Asset not found – {}", el.getCurrency());
                            throw new IllegalArgumentException(EXCEPTION_MESSAGE_ASSET_NOT_FOUND);
                        }

                        Optional<Asset> feeCurrencyOpt = assetRepository.findByTicker(el.getFeeCurrency());
                        if (feeCurrencyOpt.isPresent()) {
                            transactionRemove.setFeeAsset(feeCurrencyOpt.get());

                            Pattern feePattern = Pattern.compile("(?<=AUV: )[0-9]*.[0-9]*");
                            Matcher feeMatcher = feePattern.matcher(el.getComment());
                            if (feeMatcher.find()) {
                                transactionRemove.setFeeAmount(el.getFeeAmount().add(
                                        new BigDecimal(feeMatcher.group())));
                            } else {
                                throw new IllegalArgumentException("Failed to parse bond trade.");
                            }
                        } else {
                            log.error("Asset not found – {}", el.getFeeCurrency());
                            throw new IllegalArgumentException(EXCEPTION_MESSAGE_ASSET_NOT_FOUND);
                        }

                        Optional<Asset> assetOpt = assetRepository.findByTicker(el.getAsset());
                        if (assetOpt.isPresent()) {
                            Asset asset = assetOpt.get();
                            transactionAdd.setAsset(asset);
                            if (Objects.nonNull(asset.getNominalPrice())) {
                                transactionAdd.setAmount(el.getAmount().divide(asset.getNominalPrice()).setScale(18,
                                        RoundingMode.HALF_UP));
                            } else {
                                transactionAdd.setAmount(el.getAmount());
                            }
                        } else {
                            log.error("Asset not found, cannot create placeholder – {}", el.getAsset());
                            throw new IllegalArgumentException(EXCEPTION_MESSAGE_ASSET_NOT_FOUND);
                        }

                    } else {
                        log.error("Unknown trade type – {}", el.getComment());
                        throw new IllegalArgumentException("Unknown trade type.");
                    }

                    transactionRemove.setLocation(DEFAULT_LOCATION_NAME);
                    transactionAdd.setLocation(DEFAULT_LOCATION_NAME);

                    return Transaction.builder()
                            .type(TransactionType.TRADE)
                            .timestamp(el.getTimestamp())
                            .out(transactionRemove)
                            .in(transactionAdd)
                            .comment(el.getComment())
                            .imported(true)
                            .build();

                })
                .collect(Collectors.toList());
    }

    private List<Transaction> interests(List<FioEbrokerTransaction> transactions) {
        return transactions.stream()
                .filter(el -> el.getComment().contains(el.getAsset() + " - Dividenda") ||
                        el.getComment().contains(el.getAsset() + " - Úrokový výnos") ||
                        (el.getComment().contains("Vloženo na účet") && el.getComment().contains("27-9078470257/0100")))
                .map(el -> {

                    if (el.getComment().contains("27-9078470257/0100")) {
                        el.setAsset("BAAGECBA");
                        el.setAmount(el.getTotalAmount());
                    }

                    TransactionMovement transactionAdd = new TransactionMovement();
                    
                    Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                    if (currencyOpt.isPresent()) {
                        transactionAdd.setAsset(currencyOpt.get());
                        transactionAdd.setAmount(el.getAmount());
                    } else {
                        throw new IllegalArgumentException(EXCEPTION_MESSAGE_ASSET_NOT_FOUND);
                    }

                    Optional<Asset> sourceAssetOpt = assetRepository.findByTicker(el.getAsset());
                    if (sourceAssetOpt.isPresent()) {
                        transactionAdd.setSourceAsset(sourceAssetOpt.get());
                    }

                    transactionAdd.setLocation(DEFAULT_LOCATION_NAME);
                    transactionAdd.setExchange(defaultCurrencyExchange);

                    return Transaction.builder()
                            .type(TransactionType.INTEREST)
                            .timestamp(el.getTimestamp())
                            .in(transactionAdd)
                            .comment(el.getComment())
                            .imported(true)
                            .build();

                })
                .collect(Collectors.toList());
    }

    private List<Transaction> specialFees(List<FioEbrokerTransaction> transactions) {
        return transactions.stream()
                .filter(el -> el.getComment().contains("Daň z divid.")
                        || el.getComment().contains("Poplatek za on-line data")
                        || el.getComment().contains("Poplatek za připsání dividend"))
                .map(el -> {

                    TransactionMovement transactionRemove = new TransactionMovement();

                    Optional<Asset> currencyOpt = assetRepository.findByTicker(el.getCurrency());
                    if (currencyOpt.isPresent()) {
                        transactionRemove.setAsset(currencyOpt.get());
                        transactionRemove.setAmount(el.getTotalAmount().abs());
                    } else {
                        throw new IllegalArgumentException(EXCEPTION_MESSAGE_ASSET_NOT_FOUND);
                    }

                    if (StringUtils.isNotBlank(el.getAsset())) {
                        Optional<Asset> sourceAssetOpt = assetRepository.findByTicker(el.getAsset());
                        if (sourceAssetOpt.isPresent()) {
                            transactionRemove.setSourceAsset(sourceAssetOpt.get());
                        }
                    }

                    transactionRemove.setLocation(DEFAULT_LOCATION_NAME);
                    transactionRemove.setExchange(defaultCurrencyExchange);

                    return Transaction.builder()
                            .type(TransactionType.SPECIAL_FEE)
                            .timestamp(el.getTimestamp())
                            .out(transactionRemove)
                            .comment(el.getComment())
                            .imported(true)
                            .build();

                })
                .collect(Collectors.toList());

    }

}
