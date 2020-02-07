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

    @GetMapping("/kurzycz/file")
    public void importKurzyCzToFile(
            @RequestParam ExchangeAbbrEnum exchangeAbbr,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> to)
            throws IOException, InterruptedException {

        Exchange exchange = exchangeRepository.findByAbbreviation(exchangeAbbr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (from.isPresent() && to.isPresent()) {
            log.debug("where: {} | from: {} | to: {}", exchangeAbbr, from, to);
            importService.importKurzyCzToFile(exchange, from.get(), to.get());
        } else {
            log.debug("where: {}", exchangeAbbr);
            importService.importKurzyCzToFile(exchange);
        }
    }

    @GetMapping("/kurzycz/asset")
    public void importAssetFromKurzyCzFile() throws IOException {
        importService.importAssetFromKurzyCzFile();
    }
    
    @GetMapping("/kurzycz/price")
    public void importPriceFromKurzyCzFile() throws IOException {
        importService.importPriceFromKurzyCzFile();
    }
    
    
}
