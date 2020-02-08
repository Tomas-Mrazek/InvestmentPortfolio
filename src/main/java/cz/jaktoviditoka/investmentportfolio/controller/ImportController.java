package cz.jaktoviditoka.investmentportfolio.controller;

import cz.jaktoviditoka.investmentportfolio.domain.ExchangeAbbrEnum;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.repository.ExchangeRepository;
import cz.jaktoviditoka.investmentportfolio.security.HasAnyAuthority;
import cz.jaktoviditoka.investmentportfolio.service.ImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

@Slf4j
@HasAnyAuthority
@RestController
@RequestMapping("/import")
public class ImportController {

    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    ImportService importService;

    @GetMapping("/currencies")
    public void importCurrencies() {
        importService.importCurrencies();
    }

    @GetMapping("/kurzycz/to-file")
    public void importKurzyCzToFile(
            @RequestParam ExchangeAbbrEnum exchangeAbbr,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> to)
            throws IOException, InterruptedException {

        Exchange exchange = exchangeRepository.findByAbbreviation(exchangeAbbr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (from.isPresent() && to.isPresent()) {
            log.debug("KurzyCZ | where: {} | from: {} | to: {}", exchangeAbbr, from, to);
            importService.importKurzyCzToFile(exchange, from.get(), to.get());
        } else {
            log.debug("KurzyCZ | where: {}", exchangeAbbr);
            importService.importKurzyCzToFile(exchange);
        }
    }

    @GetMapping("/kurzycz/asset/from-file")
    public void importAssetFromKurzyCzFile(
            @RequestParam ExchangeAbbrEnum exchangeAbbr,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
            throws IOException {

        Exchange exchange = exchangeRepository.findByAbbreviation(exchangeAbbr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        importService.importAssetFromKurzyCzFile(exchange, from, to);
    }

    @GetMapping("/kurzycz/price/from-file")
    public void importPriceFromKurzyCzFile(
            @RequestParam ExchangeAbbrEnum exchangeAbbr,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
            throws IOException {

        Exchange exchange = exchangeRepository.findByAbbreviation(exchangeAbbr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        importService.importPriceFromKurzyCzFile(exchange, from, to);
    }

    @GetMapping("/kurzycz/asset")
    public void importAssetFromKurzyCz(
            @RequestParam ExchangeAbbrEnum exchangeAbbr,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date)
            throws IOException {

        Exchange exchange = exchangeRepository.findByAbbreviation(exchangeAbbr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        importService.importAssetFromKurzyCz(exchange, date);
    }

    @GetMapping("/kurzycz/price")
    public void importPriceFromKurzyCz(
            @RequestParam ExchangeAbbrEnum exchangeAbbr,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date)
            throws IOException {

        Exchange exchange = exchangeRepository.findByAbbreviation(exchangeAbbr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        importService.importPriceFromKurzyCz(exchange, date);
    }

    @GetMapping("/fio-forex/to-file")
    public void importFioForexToFile(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> to)
            throws IOException, InterruptedException {
        if (from.isPresent() && to.isPresent()) {
            log.debug("Fio Forex | from: {} | to: {}", from, to);
            importService.importFioForexToFile(from.get(), to.get());
        } else {
            log.debug("Fio Forex | ");
            importService.importFioForexToFile();
        }
    }

    @GetMapping("/fio-forex/price/from-file")
    public void importPriceFromFioForexFile(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
            throws IOException {
        importService.importPriceFromFioForexFile(from, to);
    }
    
    @GetMapping("/fio-forex/price")
    public void importPriceFromFioForexFile(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date)
            throws IOException {
        importService.importPriceFromFioForex(date);
    }

}
