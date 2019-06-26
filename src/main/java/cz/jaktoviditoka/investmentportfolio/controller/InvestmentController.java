package cz.jaktoviditoka.investmentportfolio.controller;

import cz.jaktoviditoka.investmentportfolio.entity.Asset;
import cz.jaktoviditoka.investmentportfolio.entity.Exchange;
import cz.jaktoviditoka.investmentportfolio.entity.Location;
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

import java.util.List;

@Slf4j
@HasAdminAuthority
@RestController
public class InvestmentController {

    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    ModelMapper modelMapper;

    @GetMapping("/exchanges")
    public List<Exchange> getExchanges() {
        return exchangeRepository.findAll();
    }

    @GetMapping("/exchanges/{id}")
    public Exchange getExchangesById(@RequestParam Long id) {
        return exchangeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/exchanges")
    public void createExchange(@RequestBody Exchange exchange) {
        exchangeRepository.save(exchange);
    }

    @GetMapping("/assets")
    public List<Asset> getAssets() {
        return assetRepository.findAll();
    }

    @GetMapping("/assets/{id}")
    public Asset getAssetById(@RequestParam Long id) {
        return assetRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/assets")
    public void createAsset(@RequestBody Asset asset) {
        assetRepository.save(asset);
    }

    @GetMapping("/locations")
    public List<Location> getLocations() {
        return locationRepository.findAll();
    }

    @GetMapping("/locations/{id}")
    public Location getLocationById(@RequestParam Long id) {
        return locationRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/locations")
    public void createLocation(@RequestBody Location location) {
        locationRepository.save(location);
    }

}
