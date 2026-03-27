package com.securevote.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Web3j web3j(org.springframework.core.env.Environment environment) {
        String rpcUrl = environment.getProperty("blockchain.rpc-url", "http://127.0.0.1:7545");
        return Web3j.build(new HttpService(rpcUrl));
    }
}
