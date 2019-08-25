package cz.jaktoviditoka.investmentportfolio.controller;

import cz.jaktoviditoka.investmentportfolio.domain.AssetType;
import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.AssetPrice;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.entity.Location;
import cz.jaktoviditoka.investmentportfolio.job.AssetPriceJob;
import cz.jaktoviditoka.investmentportfolio.repository.AssetPriceRepository;
import cz.jaktoviditoka.investmentportfolio.repository.AssetRepository;
import cz.jaktoviditoka.investmentportfolio.repository.ExchangeRepository;
import cz.jaktoviditoka.investmentportfolio.repository.LocationRepository;
import cz.jaktoviditoka.investmentportfolio.security.HasAdminAuthority;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Slf4j
@HasAdminAuthority
@RestController
public class BaseEntityController {

    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    AssetRepository assetRepository;
    
    @Autowired
    AssetPriceRepository assetPriceRepository;

    @Autowired
    LocationRepository locationRepository;
    
    @Autowired
    AssetPriceJob assetPrice;

    @Autowired
    ModelMapper modelMapper;

    @GetMapping("/exchanges")
    public List<Exchange> getExchanges() {
        return exchangeRepository.findAll();
    }
    
    @PostMapping("/exchanges")
    public void createExchange(@RequestBody Exchange exchange) {
        exchangeRepository.save(exchange);
    }

    @GetMapping("/exchanges/{id}")
    public Exchange getExchangesById(@RequestParam Long id) {
        return exchangeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/assets")
    public List<Asset> getAssets(@RequestParam(required = false) AssetType type) {
        if (Objects.nonNull(type)) {
            return assetRepository.findByType(type);
        } else {
            return assetRepository.findAll();
        }
    }

    @PostMapping("/assets")
    public void createAsset(@RequestBody Asset asset) {
        assetRepository.save(asset);
    }

    @GetMapping("/assets/{id}")
    public Asset getAssetById(@RequestParam Long id) {
        return assetRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    
    @GetMapping("/assets/import/priceHistory")
    public List<AssetPrice> getAssetPriceHistory() throws IOException, InterruptedException {
        assetPrice.createMissingRecords();
        return assetPriceRepository.findAll();
    }

    @GetMapping("/locations")
    public List<Location> getLocations() {
        return locationRepository.findAll();
    }
    
    @PostMapping("/locations")
    public void createLocation(@RequestBody Location location) {
        locationRepository.save(location);
    }

    @GetMapping("/locations/{id}")
    public Location getLocationById(@RequestParam Long id) {
        return locationRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

}
