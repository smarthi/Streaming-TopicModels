/*
 * Copyright 2018 Tommaso Teofili and Simone Tripodi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.github.tteofili.jtm.tm;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.github.tteofili.jtm.AnalysisUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.IOUtils;
import org.deeplearning4j.text.tokenization.tokenizer.TokenPreProcess;
import org.deeplearning4j.text.tokenization.tokenizer.Tokenizer;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;

/**
 * DL4J {@link TokenizerFactory} based on Lucene's {@link Analyzer}s
 */
public class LuceneTokenizerFactory implements TokenizerFactory {

  private final Analyzer analyzer;

  public LuceneTokenizerFactory() {
    try {
      this.analyzer = AnalysisUtils.openNLPAnalyzer();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public LuceneTokenizerFactory(Analyzer analyzer) {
    this.analyzer = analyzer;
  }

  @Override
  public Tokenizer create(String toTokenize) {
    return new LuceneTokenizer(analyzer, toTokenize);
  }

  @Override
  public Tokenizer create(InputStream toTokenize) {
    return new LuceneTokenizer(analyzer, toTokenize);
  }

  @Override
  public void setTokenPreProcessor(TokenPreProcess preProcessor) {
    // do nothing
  }

  @Override
  public TokenPreProcess getTokenPreProcessor() {
    return null;
  }

  private class LuceneTokenizer implements Tokenizer {
    private final List<String> tokens;
    private Iterator<String> iterator;

    LuceneTokenizer(Analyzer analyzer, String toTokenize) {
      TokenStream tokenStream = analyzer.tokenStream(null, toTokenize);
      tokens = new LinkedList<>();
      final CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
      try {
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
          tokens.add(charTermAttribute.toString());
        }
        tokenStream.end();
      } catch (IOException ioe) {
        throw new RuntimeException("Error occurred while iterating over tokenstream", ioe);
      } finally {
        IOUtils.closeWhileHandlingException(tokenStream);
      }

      iterator = tokens.iterator();
    }

    LuceneTokenizer(Analyzer analyzer, InputStream toTokenize) {
      TokenStream tokenStream = analyzer.tokenStream(null, new InputStreamReader(toTokenize));
      tokens = new LinkedList<>();
      final CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
      try {
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
          tokens.add(charTermAttribute.toString());
        }
        tokenStream.end();
      } catch (IOException ioe) {
        throw new RuntimeException("Error occured while iterating over tokenstream", ioe);
      } finally {
        IOUtils.closeWhileHandlingException(tokenStream);
      }

      iterator = tokens.iterator();
    }

    @Override
    public boolean hasMoreTokens() {
      return iterator.hasNext();
    }

    @Override
    public int countTokens() {
      return tokens.size();
    }

    @Override
    public String nextToken() {
      return iterator.next();
    }

    @Override
    public List<String> getTokens() {
      return tokens;
    }

    @Override
    public void setTokenPreProcessor(TokenPreProcess tokenPreProcessor) {
      // do nothing
    }
  }
}
