package cz.jaktoviditoka.investmentportfolio.config;

import cz.jaktoviditoka.investmentportfolio.dto.transaction.TransactionTradeRequest;
import cz.jaktoviditoka.investmentportfolio.entity.Transaction;
import cz.jaktoviditoka.investmentportfolio.entity.TransactionMovement;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@EnableAsync
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
            mapper.<TransactionMovement>map(src -> src.getSell(), (dest, v) -> dest.setOut(v));
            mapper.<TransactionMovement>map(src -> src.getBuy(), (dest, v) -> dest.setIn(v));
        });

        return modelMapper;
    }
    
    @Bean
    public RestTemplate getRestTemplate() {
       return new RestTemplate();
    }

}
