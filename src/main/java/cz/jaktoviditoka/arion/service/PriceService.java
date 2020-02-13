package cz.jaktoviditoka.arion.service;

import cz.jaktoviditoka.arion.dto.PriceDto;
import cz.jaktoviditoka.arion.entity.Asset;
import cz.jaktoviditoka.arion.entity.Price;
import cz.jaktoviditoka.arion.repository.AssetRepository;
import cz.jaktoviditoka.arion.repository.PriceRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PriceService {

    @Autowired
    AssetRepository assetPrice;

    @Autowired
    PriceRepository pricePrice;

    @Autowired
    ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<PriceDto> getPrice(String ticker) {
        Optional<Asset> assetOpt = assetPrice.findByTicker(ticker);
        if (assetOpt.isPresent()) {
            List<Price> prices = pricePrice.findByAsset(assetOpt.get());
            return prices.stream()
                    .map(map -> modelMapper.map(map, PriceDto.class))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
    
    @Transactional(readOnly = true)
    public List<PriceDto> getPrice(LocalDate date) {
            List<Price> prices = pricePrice.findByDate(date);
            return prices.stream()
                    .map(map -> modelMapper.map(map, PriceDto.class))
                    .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PriceDto> getPrice(String ticker, LocalDate date) {
        Optional<Asset> assetOpt = assetPrice.findByTicker(ticker);
        if (assetOpt.isPresent()) {
            List<Price> prices = pricePrice.findByAssetAndDate(assetOpt.get(), date);
            return prices.stream()
                    .map(map -> modelMapper.map(map, PriceDto.class))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

}
