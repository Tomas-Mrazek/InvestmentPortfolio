package cz.jaktoviditoka.investmentscraper.config;

import cz.jaktoviditoka.investmentscraper.dto.TransactionTradeRequest;
import cz.jaktoviditoka.investmentscraper.entity.Transaction;
import cz.jaktoviditoka.investmentscraper.entity.TransactionPart;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STANDARD);
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        
        modelMapper.createTypeMap(TransactionTradeRequest.class, Transaction.class).addMappings(mapper -> {
            mapper.<Long>skip((dest, v) -> dest.setId(v));
            mapper.<TransactionPart>map(src -> src.getSell(), (dest, v) -> dest.setFrom(v));
            mapper.<TransactionPart>map(src -> src.getBuy(), (dest, v) -> dest.setTo(v));
        });

        return modelMapper;
    }

}
