package cz.jaktoviditoka.arion.controller;

import cz.jaktoviditoka.arion.dto.PriceDto;
import cz.jaktoviditoka.arion.job.PriceJob;
import cz.jaktoviditoka.arion.repository.AssetRepository;
import cz.jaktoviditoka.arion.repository.ExchangeRepository;
import cz.jaktoviditoka.arion.repository.PriceRepository;
import cz.jaktoviditoka.arion.security.HasAnyAuthority;
import cz.jaktoviditoka.arion.service.PriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@HasAnyAuthority
@RestController
@RequestMapping("/price")
public class PriceController {
    
    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    AssetRepository assetRepository;
    
    @Autowired
    PriceRepository assetPriceRepository;

    @Autowired
    PriceJob assetPrice;
    
    @Autowired
    PriceService priceService;

    @GetMapping("/list")
    public List<PriceDto> getPrice(
            @RequestParam String ticker,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Optional<LocalDate> date) {
        if(date.isPresent()) {
            return priceService.getPrice(ticker, date.get()); 
        }
        return priceService.getPrice(ticker);
    }
    
    /*
    @GetMapping("/import")
    public List<Price> getAssetPriceHistory() throws IOException, InterruptedException {
        assetPrice.createMissingRecords();
        return assetPriceRepository.findAll();
    }

    @GetMapping("/import/priceHistorySpecific")
    public List<Price> getAssetPriceHistorySpecific(
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) Long priceAssetId,
            @RequestParam(required = false) Long exchangeId)
            throws IOException, InterruptedException {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Asset priceAsset = assetRepository.findById(priceAssetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assetPrice.createMissingRecords(asset, exchange);
        return assetPriceRepository.findByAssetAndPriceAssetAndExchange(asset, priceAsset, exchange);
    }
    */

}
