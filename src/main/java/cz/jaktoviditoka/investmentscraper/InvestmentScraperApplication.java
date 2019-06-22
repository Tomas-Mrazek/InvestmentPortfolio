package cz.jaktoviditoka.investmentscraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableJpaAuditing
@EnableJpaRepositories
@SpringBootApplication
public class InvestmentScraperApplication {
    
	public static void main(String[] args) {
		SpringApplication.run(InvestmentScraperApplication.class, args);
	}

}
