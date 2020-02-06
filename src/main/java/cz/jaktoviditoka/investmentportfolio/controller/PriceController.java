package cz.jaktoviditoka.investmentportfolio.controller;

import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.entity.Price;
import cz.jaktoviditoka.investmentportfolio.job.PriceJob;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.ExchangeRepository;
import cz.jaktoviditoka.investmentportfolio.repository.PriceRepository;
import cz.jaktoviditoka.investmentportfolio.security.HasAnyAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

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
    
}
