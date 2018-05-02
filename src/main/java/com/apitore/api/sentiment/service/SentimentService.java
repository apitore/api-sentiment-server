package com.apitore.api.sentiment.service;


import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


/**
 * @author Keigo Hattori
 */
@Service
public class SentimentService {

  @Autowired
  @Qualifier("multiLayerNetwork")
  MultiLayerNetwork net;


  public INDArray output(INDArray features, INDArray featuresMask, INDArray labelsMask) {
    INDArray predicted = net.output(features,false,featuresMask,labelsMask).dup();
    return predicted;
  }

}
