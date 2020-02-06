package cz.jaktoviditoka.investmentportfolio.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
public class PortfolioAssetPerDayResponse {

    LocalDate date;
    List<PortfolioAssetResponse> assets;
    
}
