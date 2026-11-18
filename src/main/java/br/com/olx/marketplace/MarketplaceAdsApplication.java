package br.com.olx.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MarketplaceAdsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketplaceAdsApplication.class, args);
    }
}
