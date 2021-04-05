package cz.tomastokamrazek.arion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.Getter;

@Getter
@Configuration
@PropertySource("classpath:secrets.properties")
public class SecretsConfig {

	@Value("${coinmarketcap.api.key}")
	private String coinmarketcapApiKey;
	
	@Value("${kraken.api.key}")
	private String krakenApiKey;

	@Value("${kraken.private.key}")
	private String krakenPrivateKey;

}
