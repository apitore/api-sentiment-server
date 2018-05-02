package com.apitore.api.sentiment;


import java.io.IOException;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.annotation.RequestScope;


/**
 * @author Keigo Hattori
 */
@EnableFeignClients
@EnableDiscoveryClient
@EnableCircuitBreaker
@EnableZuulProxy
@SpringBootApplication
public class SentimentAppMain {

  public static void main(String[] args) {
    SpringApplication.run(SentimentAppMain.class, args);
  }

  // FIXME MultiLayerNetworkがスレッドアンセーフのため@Scopeを設置
  @RequestScope
  @Bean(name="multiLayerNetwork", destroyMethod="clear")
  public MultiLayerNetwork multiLayerNetwork() throws IOException, ClassNotFoundException {
    MultiLayerNetwork net =
        ModelSerializer.restoreMultiLayerNetwork("sentiment.rnn.model",false);
    return net;
  }

}