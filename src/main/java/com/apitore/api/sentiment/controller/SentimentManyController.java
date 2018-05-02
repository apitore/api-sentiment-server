package com.apitore.api.sentiment.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apitore.api.sentiment.service.KuromojiIntegration;
import com.apitore.api.sentiment.service.SentimentService;
import com.apitore.api.sentiment.service.Word2VecIntegration;
import com.apitore.banana.request.sentiment.SentimentRequestEntity;
import com.apitore.banana.response.sentiment.ListSentimentResponseEntity;
import com.apitore.banana.response.sentiment.SentimentResponseEntity;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;


/**
 * @author Keigo Hattori
 */
@RestController
@RequestMapping(value = "/sentiments")
public class SentimentManyController extends SentimentBase {

  @Autowired
  Word2VecIntegration word2VecIntegration;
  @Autowired
  SentimentService    sentimentService;
  @Autowired
  KuromojiIntegration kuromojiIntegration;

  final String NOTES = "Sentiment Analysis, last update at 2017-5-12.<BR />"
      + "Response<BR />"
      + "&nbsp; Github: <a href=\"https://github.com/keigohtr/apitore-response-parent/tree/master/word2vec-response\">word2vec-response</a><BR />"
      + "&nbsp; Class: com.apitore.banana.response.sentiment.ListSentimentResponseEntity<BR />";

  /**
   * 実態
   *
   * @param texts
   * @return
   */
  @RequestMapping(value="/open/predict", method=RequestMethod.POST)
  @ApiIgnore
  public ResponseEntity<ListSentimentResponseEntity> predict(
      @RequestBody SentimentRequestEntity req
      ) {

    ListSentimentResponseEntity model = new ListSentimentResponseEntity();
    Long startTime = System.currentTimeMillis();
    List<SentimentResponseEntity> sentimentlist = predictSentiment(req.getTexts());
    model.setSentimentlist(sentimentlist);
    model.setLog("Success.");
    Long endTime = System.currentTimeMillis();
    Long processTime = endTime-startTime;
    model.setStartTime(startTime.toString());
    model.setEndTime(endTime.toString());
    model.setProcessTime(processTime.toString());
    return new ResponseEntity<ListSentimentResponseEntity>(model,HttpStatus.OK);
  }

  /**
   * 公開用API
   * Dummyメソッド
   *
   * @param access_token
   * @param test
   * @return
   */
  @RequestMapping(value = {"/predict"}, produces=MediaType.APPLICATION_JSON_VALUE, method=RequestMethod.POST)
  @ApiOperation(value="Sentiment predict", notes=NOTES)
  public ListSentimentResponseEntity predict(
      @ApiParam(value = "Access Token", required = true)
      @RequestParam("access_token")  String access_token,
      @RequestBody SentimentRequestEntity req)
  {
    return new ListSentimentResponseEntity();
  }

}
