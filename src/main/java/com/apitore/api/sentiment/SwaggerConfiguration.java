package com.apitore.api.sentiment;


import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

import java.util.Date;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


/**
 * @author Keigo Hattori
 */
@Configuration
@EnableSwagger2
public class SwaggerConfiguration {

  @SuppressWarnings("unchecked")
  @Bean
  public Docket sentimentAPI() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("sentiment")
        .select()
        .apis(RequestHandlerSelectors.basePackage("com.apitore.api.sentiment.controller"))
        .paths(or(
            regex(".*/sentiment/.*")
            ))
        .build()
        .apiInfo(
            new ApiInfoBuilder()
            .title("Sentiment APIs")
            .description("Japanese sentiment analyzer.")
            .version("0.0.1")
            .build()
            )
        .directModelSubstitute(Date.class, Long.class);
  }

  @SuppressWarnings("unchecked")
  @Bean
  public Docket sentimentEnterpriseAPI() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("sentiments")
        .select()
        .apis(RequestHandlerSelectors.basePackage("com.apitore.api.sentiment.controller"))
        .paths(or(
            regex(".*/sentiments/.*")
            ))
        .build()
        .apiInfo(
            new ApiInfoBuilder()
            .title("Sentiment APIs")
            .description("Japanese sentiment analyzer.")
            .version("0.0.1")
            .build()
            )
        .directModelSubstitute(Date.class, Long.class);
  }

}