package cz.jaktoviditoka.investmentportfolio.config;

import cz.jaktoviditoka.investmentportfolio.dto.TransactionTradeRequest;
import cz.jaktoviditoka.investmentportfolio.entity.Transaction;
import cz.jaktoviditoka.investmentportfolio.entity.TransactionPart;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "cz.jaktoviditoka.investmentportfolio.repository")
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
