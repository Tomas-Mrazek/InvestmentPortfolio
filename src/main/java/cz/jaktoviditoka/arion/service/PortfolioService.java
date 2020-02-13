package cz.jaktoviditoka.arion.service;

import cz.jaktoviditoka.arion.domain.PortfolioAssetPerDay;
import cz.jaktoviditoka.arion.dto.*;
import cz.jaktoviditoka.arion.entity.AppUser;
import cz.jaktoviditoka.arion.model.PortfolioManagement;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.image.WritableImage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

@Slf4j
@Transactional
@Service
public class PortfolioService {

    @Autowired
    PriceService priceService;

    @Autowired
    PortfolioManagement portfolio;

    @Autowired
    ModelMapper modelMapper;

    public List<PortfolioAssetPerDayResponse> getPortfolioPerDay(AppUser appuser, LocalDate date) {
        return getPortfolioPerDay(appuser).stream()
                .filter(el -> Objects.equals(el.getDate(), date))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PortfolioAssetPerDayResponse> getPortfolioPerDay(AppUser appuser) {
        List<PortfolioAssetPerDay> listOfPaapd = portfolio.portfolioPerDay(appuser);
        List<PortfolioAssetPerDayResponse> listOfPaapdDto = new ArrayList<>();

        listOfPaapd.stream().forEach(el -> {
            PortfolioAssetPerDayResponse paapdDto = PortfolioAssetPerDayResponse.builder()
                    .date(el.getDate())
                    .assets(new ArrayList<>())
                    .build();

            el.getAssets().stream().forEach(asset -> {
                paapdDto.getAssets().add(modelMapper.map(asset, PortfolioAssetResponse.class));
            });

            paapdDto.getAssets().sort(Comparator
                    .comparing(PortfolioAssetResponse::getAssetType)
                    .thenComparing(PortfolioAssetResponse::getAssetName));

            listOfPaapdDto.add(paapdDto);
        });

        return listOfPaapdDto.stream()
                .sorted(Comparator.comparing(PortfolioAssetPerDayResponse::getDate).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PortfolioHistoryDayDto> portfolioHistory(AppUser appuser) {
        return portfolio.portfolioHistory(appuser).stream()
                .sorted(Comparator
                        .comparing(PortfolioHistoryDay::getDate)
                        .reversed())
                .map(portfolioHistoryDay -> {
                    return modelMapper.map(portfolioHistoryDay, PortfolioHistoryDayDto.class);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PortfolioDayValueDto> portfolioValue(AppUser appuser) {
        return portfolio.portfolioValue(appuser);
    }

    @Transactional(readOnly = true)
    public ByteArrayOutputStream portfolioValuePrint(AppUser appuser) throws InterruptedException {
        MutableBoolean done = new MutableBoolean(false);
        
        List<PortfolioDayValueDto> portfolioValue = portfolio.portfolioValue(appuser);
        LocalDate minDate = portfolioValue.stream()
                .min((a, b) -> a.getDate().compareTo(b.getDate()))
                .orElseThrow()
                .getDate();

        portfolioValue.stream()
                .forEach(el -> el.setDate(el.getDate().minusDays(minDate.toEpochDay())));
        
        List<PortfolioDayValueDto> portfolioEarnings = portfolio.portfolioEarnings(appuser);
        portfolioEarnings.stream()
            .forEach(el -> el.setDate(el.getDate().minusDays(minDate.toEpochDay())));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Platform.startup(() -> {
            // defining the axes
            NumberAxis xAxis = new NumberAxis();
            NumberAxis yAxis = new NumberAxis();
            xAxis.setLabel("Days");
            yAxis.setLabel("Value");
            // creating the chart
            LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle("Portfolio");
            lineChart.setCreateSymbols(false);
            // defining a series
            XYChart.Series<Number, Number> totalValue = new XYChart.Series<>();
            totalValue.setName("Total value");
            // populating the series with data
            portfolioValue.stream()
                    .forEach(portfolioValueDay -> {
                        long epochDay = portfolioValueDay.getDate().toEpochDay();
                        BigDecimal value = portfolioValueDay.getValue();
                        log.debug("epochDay: {}, value: {}", epochDay, value);
                        totalValue.getData().add(new XYChart.Data<>(epochDay, value));
                    });
            // defining a series
            XYChart.Series<Number, Number> earnings = new XYChart.Series<>();
            earnings.setName("Earnings");
            // populating the series with data
            portfolioEarnings.stream()
                    .forEach(portfolioEarningsDay -> {
                        long epochDay = portfolioEarningsDay.getDate().toEpochDay();
                        BigDecimal value = portfolioEarningsDay.getValue();
                        log.debug("epochDay: {}, value: {}", epochDay, value);
                        earnings.getData().add(new XYChart.Data<>(epochDay, value));
                    });
            // add series to linechart
            lineChart.getData().add(totalValue);
            lineChart.getData().add(earnings);
            Scene scene = new Scene(lineChart, 800, 600);
            WritableImage image = scene.snapshot(null);

            try {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                ImageIO.write(bufferedImage, "png", baos);
                log.debug("ByteArrayOutputStream size: {}", baos.size());
                done.setTrue();
            } catch (IOException e) {
                log.debug("{}", e);
            }

        });

        while (done.isFalse()) {
            Thread.sleep(100);
        }

        return baos;
    }

}
