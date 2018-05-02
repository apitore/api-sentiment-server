package com.apitore.api.sentiment.service;


import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.apitore.banana.request.word2vec.WordsRequestEntity;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;


@FeignClient("api-word2vec-server")
interface Word2VecClient {
  @RequestMapping(method = RequestMethod.GET, value="/word2vec-neologd-jawiki/open/hasword")
  ResponseEntity<Boolean> hasword(
      @RequestParam("word")
      String word);

  @RequestMapping(method = RequestMethod.GET, value="/word2vec-neologd-jawiki/open/layersize")
  ResponseEntity<Integer> layersize();

  @RequestMapping(method = RequestMethod.POST, value="/word2vec-neologd-jawiki/open/wordvectormatrix")
  ResponseEntity<Map<String,double[]>> wordvectormatrix(
      @RequestBody
      WordsRequestEntity req);
}

@Component
public class Word2VecIntegration {

  @Autowired
  Word2VecClient word2vecClient;


  public ResponseEntity<Boolean> haswordFallback(
      String  word) {
    return new ResponseEntity<Boolean>(false,HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public ResponseEntity<Integer> layersizeFallback() {
    return new ResponseEntity<Integer>(-1,HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public ResponseEntity<Map<String,double[]>> wordvectormatrixFallback(
      WordsRequestEntity req) {
    Map<String,double[]> rtn = new HashMap<>();
    return new ResponseEntity<Map<String,double[]>>(rtn,HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @HystrixCommand(fallbackMethod = "haswordFallback")
  public ResponseEntity<Boolean> hasWord(
      String  word) {
    return word2vecClient.hasword(word);
  }

  @HystrixCommand(fallbackMethod = "layersizeFallback")
  public ResponseEntity<Integer> layerSize() {
    return word2vecClient.layersize();
  }

  @HystrixCommand(fallbackMethod = "wordvectormatrixFallback")
  public ResponseEntity<Map<String,double[]>> getWordVectorMatrix(
      WordsRequestEntity req) {
    return word2vecClient.wordvectormatrix(req);
  }
}
