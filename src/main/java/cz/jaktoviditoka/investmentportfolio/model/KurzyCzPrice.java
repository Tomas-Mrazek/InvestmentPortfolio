package cz.jaktoviditoka.investmentportfolio.model;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class KurzyCzPrice {

    String date;
    List<KurzyCzPriceDay> listOfPrices;
    
}
