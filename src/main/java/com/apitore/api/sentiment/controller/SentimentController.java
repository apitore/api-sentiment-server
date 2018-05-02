package com.apitore.api.sentiment.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apitore.api.sentiment.service.KuromojiIntegration;
import com.apitore.api.sentiment.service.SentimentService;
import com.apitore.api.sentiment.service.Word2VecIntegration;
import com.apitore.banana.response.sentiment.SentimentResponseEntity;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;


/**
 * @author Keigo Hattori
 */
@RestController
@RequestMapping(value = "/sentiment")
public class SentimentController extends SentimentBase {

  @Autowired
  Word2VecIntegration word2VecIntegration;
  @Autowired
  SentimentService    sentimentService;
  @Autowired
  KuromojiIntegration kuromojiIntegration;

  final String NOTES = "Sentiment Analysis, last update at 2017-5-12.<BR />"
      + "Response<BR />"
      + "&nbsp; Github: <a href=\"https://github.com/keigohtr/apitore-response-parent/tree/master/word2vec-response\">word2vec-response</a><BR />"
      + "&nbsp; Class: com.apitore.banana.response.sentiment.SentimentResponseEntity<BR />";

  /**
   * 実態
   *
   * @param text
   * @return
   */
  @RequestMapping(value="/open/predict", method=RequestMethod.GET)
  @ApiIgnore
  public ResponseEntity<SentimentResponseEntity> predict(
      @RequestParam("text") String text
      ) {

    Long startTime = System.currentTimeMillis();
    SentimentResponseEntity model = predictSentiment(text);

    if (!model.isError())
      model.setLog("Success.");
    Long endTime = System.currentTimeMillis();
    Long processTime = endTime-startTime;
    model.setStartTime(startTime.toString());
    model.setEndTime(endTime.toString());
    model.setProcessTime(processTime.toString());
    return new ResponseEntity<SentimentResponseEntity>(model,HttpStatus.OK);
  }

  /**
   * 公開用API
   * Dummyメソッド
   *
   * @param access_token
   * @param test
   * @return
   */
  @RequestMapping(value = {"/predict"}, produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.GET)
  @ApiOperation(value="Sentiment predict", notes=NOTES)
  public SentimentResponseEntity predict(
      @ApiParam(value = "Access Token", required = true)
      @RequestParam("access_token")  String access_token,
      @ApiParam(value = "text", required = true)
      @RequestParam("text")       String text)
  {
    return new SentimentResponseEntity();
  }

}
