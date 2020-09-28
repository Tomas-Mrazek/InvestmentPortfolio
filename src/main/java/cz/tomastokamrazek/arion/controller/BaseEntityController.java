package cz.tomastokamrazek.arion.controller;

import cz.tomastokamrazek.arion.domain.AssetType;
import cz.tomastokamrazek.arion.dto.AssetDto;
import cz.tomastokamrazek.arion.entity.Asset;
import cz.tomastokamrazek.arion.entity.Exchange;
import cz.tomastokamrazek.arion.repository.AssetRepository;
import cz.tomastokamrazek.arion.repository.ExchangeRepository;
import cz.tomastokamrazek.arion.security.HasAdminAuthority;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@HasAdminAuthority
@RestController
public class BaseEntityController {

    @Autowired
    ExchangeRepository exchangeRepository;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    ModelMapper modelMapper;

    @GetMapping("/exchanges")
    public List<Exchange> getExchanges() {
        return exchangeRepository.findAll();
    }

    @PostMapping("/exchanges")
    public Exchange createExchange(@RequestBody Exchange exchange) {
        return exchangeRepository.save(exchange);
    }

    @GetMapping("/exchanges/{id}")
    public Exchange getExchangesById(@RequestParam Long id) {
        return exchangeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/assets")
    public List<Asset> getAssets(@RequestParam Optional<AssetType> type) {
        if (type.isPresent()) {
            return assetRepository.findByType(type.get());
        } else {
            return assetRepository.findAll();
        }
    }

    @GetMapping("/asset")
    public AssetDto getAsset(@RequestParam String ticker) {
        Asset asset = assetRepository.findByTicker(ticker)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found."));

        if (Objects.nonNull(asset.getNominalPriceAsset())) {
            return AssetDto.builder()
                    .name(asset.getName())
                    .ticker(asset.getTicker())
                    .isin(asset.getIsin())
                    .type(asset.getType().name())
                    .nominalPriceAsset(asset.getNominalPriceAsset().getTicker())
                    .nominalPrice(asset.getNominalPrice())
                    .build();
        } else {
            return AssetDto.builder()
                    .name(asset.getName())
                    .ticker(asset.getTicker())
                    .isin(asset.getIsin())
                    .type(asset.getType().name())
                    .build();
        }

    }

    @PostMapping("/asset")
    public void createAsset(@RequestBody AssetDto assetDto) {
        Asset nominalPriceAsset = assetRepository.findByTicker(assetDto.getNominalPriceAsset())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found."));

        Asset asset = Asset.builder()
                .name(assetDto.getName())
                .ticker(assetDto.getTicker())
                .isin(assetDto.getIsin())
                .type(AssetType.valueOf(assetDto.getType()))
                .nominalPriceAsset(nominalPriceAsset)
                .nominalPrice(assetDto.getNominalPrice())
                .build();
        assetRepository.save(asset);
    }

    @PutMapping("/asset")
    public void updateAsset( @RequestBody AssetDto assetDto) {
        Asset asset = assetRepository.findByTicker(assetDto.getTicker())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found."));

        Asset nominalPriceAsset = assetRepository.findByTicker(assetDto.getNominalPriceAsset())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found."));

        asset = Asset.builder()
                .id(asset.getId())
                .name(assetDto.getName())
                .ticker(assetDto.getTicker())
                .isin(assetDto.getIsin())
                .type(AssetType.valueOf(assetDto.getType()))
                .nominalPriceAsset(nominalPriceAsset)
                .nominalPrice(assetDto.getNominalPrice())
                .build();
        assetRepository.save(asset);
    }

    @GetMapping("/asset/{id}")
    public Asset getAssetById(@RequestParam Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found."));
    }

}
