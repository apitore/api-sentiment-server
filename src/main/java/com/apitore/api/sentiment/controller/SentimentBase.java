package com.apitore.api.sentiment.controller;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.apitore.api.sentiment.service.KuromojiIntegration;
import com.apitore.api.sentiment.service.SentimentService;
import com.apitore.api.sentiment.service.Word2VecIntegration;
import com.apitore.banana.request.com.atilika.kuromoji.KuromojiRequestEntity;
import com.apitore.banana.request.word2vec.WordsRequestEntity;
import com.apitore.banana.response.com.atilika.kuromoji.TokenEntity;
import com.apitore.banana.response.com.atilika.kuromoji.TokensResponseEntity;
import com.apitore.banana.response.sentiment.SentimentEntity;
import com.apitore.banana.response.sentiment.SentimentResponseEntity;


/**
 * @author Keigo Hattori
 */
@RestController
public class SentimentBase {

  private final Logger LOG = Logger.getLogger(SentimentBase.class);

  @Autowired
  Word2VecIntegration word2VecIntegration;
  @Autowired
  SentimentService    sentimentService;
  @Autowired
  KuromojiIntegration kuromojiIntegration;

  /**
   * predictSentiment
   *
   * @param entity
   * @return
   */
  protected SentimentResponseEntity predictSentiment(String text) {
    List<SentimentResponseEntity> entities = predictSentiment(Arrays.asList(text));
    return entities.get(0);
  }
  /**
   * predictSentiment
   *
   * @param entities
   * @return
   */
  protected List<SentimentResponseEntity> predictSentiment(List<String> texts) {
    List<SentimentResponseEntity> rtn = new ArrayList<>();
    List<List<String>> allTokens = new ArrayList<>();
    int maxLength = 0;
    int count     = 0;

    ResponseEntity<Integer> res1 = word2VecIntegration.layerSize();
    if (res1.getStatusCode() != HttpStatus.OK) {
      LOG.error("word2vec layersize error.");
      SentimentResponseEntity e = new SentimentResponseEntity();
      e.setError(true);
      e.setLog("Internal Server Error: \"word2vec\"");
      rtn.add(e);
      return rtn;
    }
    int layerSize = res1.getBody();

    List<String> norm_texts = new ArrayList<>();
    for (String s: texts) {
      s = s.replaceAll("http://[\\S]+", "URL");
      s = s.replaceAll("@[\\w\\d_]+", "USER");
      s = s.replaceAll("#[\\S]+", "TAG");
      norm_texts.add(s);
    }

    KuromojiRequestEntity kent = new KuromojiRequestEntity();
    kent.setTexts(norm_texts);
    ResponseEntity<TokensResponseEntity> res2 = kuromojiIntegration.kuromojiIpadicNeologdTokenize(kent);
    if (res2.getStatusCode() != HttpStatus.OK) {
      LOG.error("kuromoji error.");
      SentimentResponseEntity e = new SentimentResponseEntity();
      e.setError(true);
      e.setLog("Internal Server Error: \"kuromoji\"");
      rtn.add(e);
      return rtn;
    }

    for (int i=0; i<texts.size(); i++) {
      String text = texts.get(i);
      List<TokenEntity> tokens = res2.getBody().getTokens().get(i);
      SentimentResponseEntity e = new SentimentResponseEntity();
      e.setText(text);
      List<String> tokensFiltered = new ArrayList<>();
      boolean chk;
      chk = true;
      for(TokenEntity t : tokens ){
        String str = t.getSurface();
        ResponseEntity<Boolean> res3 = word2VecIntegration.hasWord(str);
        if (res3.getStatusCode() != HttpStatus.OK)
          chk=false;
        if (res3.getBody())
          tokensFiltered.add(str);
      }
      if (!chk) {
        LOG.error("word2vec hasword error.");
        e.setError(true);
        e.setLog("Internal Server Error.");
        continue;
      } else if (tokensFiltered.isEmpty()) {
        LOG.error("All OOV words..");
        e.setError(true);
        e.setLog("All OOV words: No."+count);
        continue;
      }
      rtn.add(e);
      allTokens.add(tokensFiltered);
      maxLength = Math.max(maxLength,tokensFiltered.size());
    }
    if (maxLength==0 || rtn.isEmpty())
      return rtn;

    WordsRequestEntity ent = new WordsRequestEntity();
    for (int i=0; i<rtn.size(); i++) {
      ent.getWords().addAll(allTokens.get(i));
    }
    ResponseEntity<Map<String, double[]>> res4 = word2VecIntegration.getWordVectorMatrix(ent);
    if (res4.getStatusCode() != HttpStatus.OK) {//FIXME どう処理すれば良いか・・・。
      LOG.error("word2vec getwordvector error.");
      return rtn;
    }
    Map<String,double[]> vecmap = res4.getBody();

    INDArray features = Nd4j.create(rtn.size(), layerSize, maxLength);
    INDArray featuresMask = Nd4j.zeros(rtn.size(), maxLength);
    INDArray labelsMask = Nd4j.zeros(rtn.size(), maxLength);
    int[] temp = new int[2];
    for (int i=0; i<rtn.size(); i++) {
      List<String> tokens = allTokens.get(i);
      temp[0] = i;
      for( int j=0; j<tokens.size() && j<maxLength; j++ ){
        String token = tokens.get(j).toLowerCase();
        INDArray vector;
        if (vecmap.containsKey(token))
          vector = Nd4j.create(vecmap.get(token)).dup();
        else {
          LOG.error("Invalid term: "+token);
          vector = Nd4j.zeros(layerSize);
        }
        features.put(new INDArrayIndex[]{NDArrayIndex.point(i), NDArrayIndex.all(), NDArrayIndex.point(j)}, vector);
        temp[1] = j;
        featuresMask.putScalar(temp, 1.0);
      }
      int lastIdx = Math.min(tokens.size(),maxLength);
      labelsMask.putScalar(new int[]{i,lastIdx-1},1.0);
    }
    INDArray predicted = sentimentService.output(features, featuresMask, labelsMask);

    int totalOutputExamples = labelsMask.sumNumber().intValue();
    int outSize = 3;// FIXME positive or negative or neutral
    INDArray predicted2d = Nd4j.create(totalOutputExamples, outSize);
    int rowCount = 0;
    for (int ex = 0; ex < labelsMask.size(0); ++ex) {
      for (int t = 0; t < labelsMask.size(1); ++t) {
        if (labelsMask.getDouble(ex, t) == 0.0D)
          continue;
        predicted2d.putRow(rowCount,
            predicted.get(new INDArrayIndex[] { NDArrayIndex.point(ex), NDArrayIndex.all(), NDArrayIndex.point(t) }));
        ++rowCount;
      }
    }

    // FIXME positive or negative or neutral
    for (int r=0; r<rowCount; r++) {
      SentimentResponseEntity e = rtn.get(r);
      SentimentEntity positive = new SentimentEntity();
      SentimentEntity negative = new SentimentEntity();
      SentimentEntity neutral  = new SentimentEntity();
      SentimentEntity predict  = new SentimentEntity();

      double posscore = predicted2d.getDouble(r, 0);
      double negscore = predicted2d.getDouble(r, 1);
      double neuscore = predicted2d.getDouble(r, 2);
      positive.setSentiment("positive");
      positive.setScore(posscore);
      negative.setSentiment("negative");
      negative.setScore(negscore);
      neutral.setSentiment("neutral");
      neutral.setScore(neuscore);
      List<SentimentEntity> se = Arrays.asList(positive,negative,neutral);
      e.setSentiments(se);

      if (posscore>negscore) {
        if (posscore>neuscore) {
          predict.setSentiment("positive");
          predict.setScore(posscore);
        } else {
          predict.setSentiment("neutral");
          predict.setScore(neuscore);
        }
      } else if (negscore>posscore) {
        if (negscore>neuscore) {
          predict.setSentiment("negative");
          predict.setScore(negscore);
        } else {
          predict.setSentiment("neutral");
          predict.setScore(neuscore);
        }
      } else {
        predict.setSentiment("neutral");
        predict.setScore(neuscore);
      }
      e.setPredict(predict);
    }

    return rtn;
  }

}
