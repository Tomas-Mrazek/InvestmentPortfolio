package cz.tomastokamrazek.arion.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.tomastokamrazek.arion.dto.transaction.TransactionTradeRequest;
import cz.tomastokamrazek.arion.entity.Transaction;
import cz.tomastokamrazek.arion.entity.TransactionMovement;
import org.flywaydb.core.Flyway;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;

@EnableAsync
@EnableScheduling
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "cz.tomastokamrazek.arion.repository")
@Configuration
public class Config {

    @Bean
    @Profile("cleandb")
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        flyway.clean();
        flyway.migrate();
        return flyway;
    }
    
    @Bean
    public ObjectMapper createObjectMapper() {  
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
    
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
    	RestTemplate restTemplate = new RestTemplate();
    	restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    	return restTemplate;
    }

}
