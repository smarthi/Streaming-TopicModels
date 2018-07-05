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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TreeMap;

import com.github.tteofili.jtm.feed.Comment;
import com.github.tteofili.jtm.feed.Issue;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.embeddings.learning.SequenceLearningAlgorithm;
import org.deeplearning4j.models.embeddings.learning.impl.sequence.DM;
import org.deeplearning4j.models.embeddings.reader.impl.BasicModelUtils;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.sequencevectors.sequence.Sequence;
import org.deeplearning4j.models.sequencevectors.sequence.SequenceElement;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.text.documentiterator.LabelledDocument;
import org.deeplearning4j.text.documentiterator.LabelsSource;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.ops.transforms.Transforms;

import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * Simplified version of Par2Hier implementation for DL4j for {@link com.github.tteofili.jtm.feed.Issue}s
 *
 */
public class Par2Hier extends ParagraphVectors {

  private static final long serialVersionUID = 1383866565833005689L;

  @Getter
  protected LabelsSource labelsSource;
  @Getter
  @Setter
  protected transient IssueIterator labelAwareIterator;
  protected INDArray labelsMatrix;
  protected List<VocabWord> labelsList = new ArrayList<>();
  protected boolean normalizedLabels = false;
  private Method smoothing;
  private Integer k;
  private WeightLookupTable<VocabWord> lookupTable;
  private VocabCache<VocabWord> vocab;

  Par2Hier(ParagraphVectors paragraphVectors, Method smoothing, int k) {
    this.smoothing = smoothing;
    this.k = k;

    this.labelsSource = paragraphVectors.getLabelsSource();
    this.labelAwareIterator = (IssueIterator) paragraphVectors.getLabelAwareIterator();
    this.vocab = paragraphVectors.getVocab();

    @SuppressWarnings("unchecked") // verified in source code
    WeightLookupTable<? extends SequenceElement> parLT = paragraphVectors.lookupTable();
    this.lookupTable = rebuildLookupTable(parLT, this.vocab);

    this.tokenizerFactory = paragraphVectors.getTokenizerFactory();
    this.modelUtils = paragraphVectors.getModelUtils();

  }

  private static <T extends SequenceElement> WeightLookupTable<VocabWord> rebuildLookupTable(WeightLookupTable<T> parLT, VocabCache<VocabWord> vocabCache) {
    WeightLookupTable<VocabWord> lookupTable = new InMemoryLookupTable.Builder<VocabWord>()
        .vectorLength(parLT.layerSize()).cache(vocabCache).build();
    lookupTable.resetWeights();

    for (String w : vocabCache.words()) {
      INDArray vector = parLT.vector(w);
      lookupTable.putVector(w, vector);
    }
    return lookupTable;
  }


  @Override
  public String toString() {
    return "Par2Hier{" +
        "smoothing=" + smoothing +
        ", k=" + k +
        '}';
  }

  public void extractLabels() {
    Collection<VocabWord> vocabWordCollection = vocab.vocabWords();
    List<VocabWord> vocabWordList = new ArrayList<>();
    int[] indexArray;

    //INDArray pulledArray;
    //Check if word has label and build a list out of the collection
    for (VocabWord vWord : vocabWordCollection) {
      if (vWord.isLabel()) {
        vocabWordList.add(vWord);
      }
    }
    //Build array of indexes in the order of the vocablist
    indexArray = new int[vocabWordList.size()];
    int i = 0;
    for (VocabWord vWord : vocabWordList) {
      indexArray[i] = vWord.getIndex();
      i++;
    }
    //pull the label rows and create new matrix
    if (i > 0) {
      labelsMatrix = Nd4j.pullRows(lookupTable.getWeights(), 1, indexArray);
      labelsList = vocabWordList;
    }
  }

  /**
   * This method calculates inferred vector for given text
   *
   */
  public INDArray inferVector(String text, double learningRate, double minLearningRate, int iterations) {
    if (tokenizerFactory == null) {
      throw new IllegalStateException("TokenizerFactory should be defined, prior to predict() call");
    }

    List<String> tokens = tokenizerFactory.create(text).getTokens();
    List<VocabWord> document = new ArrayList<>();
    for (String token : tokens) {
      if (vocab.containsWord(token)) {
        document.add(vocab.wordFor(token));
      }
    }

    return inferVector(document, learningRate, minLearningRate, iterations);
  }

  /**
   * This method calculates inferred vector for given document
   * @param document the document to extract the vector for
   * @param iterations the no. of iterations used to infer the sequence
   * @param learningRate the start learning rate
   * @param minLearningRate the minimum learning rate
   * @return a vector representation of the document
   */
  public INDArray inferVector(LabelledDocument document, double learningRate, double minLearningRate, int iterations) {
    if (document.getReferencedContent() != null) {
      return inferVector(document.getReferencedContent(), learningRate, minLearningRate, iterations);
    } else {
      return inferVector(document.getContent(), learningRate, minLearningRate, iterations);
    }
  }

  /**
   * This method calculates inferred vector for given document represented as a list of words
   *
   * @param document the document to extract the vector for
   * @param iterations the no. of iterations used to infer the sequence
   * @param learningRate the start learning rate
   * @param minLearningRate the minimum learning rate
   * @return a vector representation of the document
   */
  public INDArray inferVector(List<VocabWord> document, double learningRate, double minLearningRate, int iterations) {
    SequenceLearningAlgorithm<VocabWord> learner = sequenceLearningAlgorithm;

    if (learner == null) {
      log.info("Creating new PV-DM learner...");
      learner = new DM<>();
      learner.configure(vocab, lookupTable, configuration);
    }

    Sequence<VocabWord> sequence = new Sequence<>();
    sequence.addElements(document);
    sequence.setSequenceLabel(new VocabWord(1.0, String.valueOf(new Random().nextInt())));

        /*
        for (int i = 0; i < iterations; i++) {
            sequenceLearningAlgorithm.learnSequence(sequence, new AtomicLong(0), learningRate);
        }*/

    initLearners();

    return learner.inferSequence(sequence, 119, learningRate, minLearningRate, iterations);
  }

  /**
   * This method calculates inferred vector for given text, with default parameters for learning rate and iterations
   *
   * @param text the text to extract the vector for
   * @return a vector representation for the given text
   */
  public INDArray inferVector(String text) {
    return inferVector(text, this.learningRate.get(), this.minLearningRate, this.numEpochs * this.numIterations);
  }

  /**
   * This method calculates inferred vector for given document, with default parameters for learning rate and iterations
   *
   * @param document a document to extract the vector for
   * @return a vector representation of the given document
   */
  public INDArray inferVector(LabelledDocument document) {
    return inferVector(document, this.learningRate.get(), this.minLearningRate, this.numEpochs * this.numIterations);
  }

  /**
   * This method calculates inferred vector for given list of words, with default parameters for learning rate and iterations
   *
   * @param document a document to extract the vector for
   * @return a vector representation of the given document
   */
  public INDArray inferVector(List<VocabWord> document) {
    return inferVector(document, this.learningRate.get(), this.minLearningRate, this.numEpochs * this.numIterations);
  }


  /**
   * This method returns top N labels nearest to specified document
   *
   * @param document a document
   * @param topN no. of nearest labels to find
   * @return a {@code Collection} of the nearest labels to the given document
   */
  public Collection<String> nearestLabels(LabelledDocument document, int topN) {
    if (document.getReferencedContent() != null) {
      return nearestLabels(document.getReferencedContent(), topN);
    } else {
      return nearestLabels(document.getContent(), topN);
    }
  }

  /**
   * This method returns top N labels nearest to specified text
   *
   * @param rawText a raw text
   * @param topN no. of nearest labels to find
   * @return a {@code Collection} of the nearest labels to the given text
   */
  public Collection<String> nearestLabels(String rawText, int topN) {
    List<String> tokens = tokenizerFactory.create(rawText).getTokens();
    List<VocabWord> document = new ArrayList<>();
    for (String token : tokens) {
      if (vocab.containsWord(token)) {
        document.add(vocab.wordFor(token));
      }
    }
    return nearestLabels(document, topN);
  }

  /**
   * This method returns top N labels nearest to specified set of vocab words
   *
   * @param document a document
   * @param topN no. of nearest labels to find
   * @return a {@code Collection} of the nearest labels to the given document
   */
  public Collection<String> nearestLabels(Collection<VocabWord> document, int topN) {
    INDArray vector = inferVector(new ArrayList<>(document));
    return nearestLabels(vector, topN);
  }

  /**
   * This method returns top N labels nearest to specified features vector
   *
   * @param labelVector a vector for the source label
   * @param topN no. of nearest labels to find
   * @return a {@code Collection} of the nearest labels to the given document
   */
  public Collection<String> nearestLabels(INDArray labelVector, int topN) {
    List<BasicModelUtils.WordSimilarity> result = new ArrayList<>();

    // if list still empty - return empty collection
    if (labelsMatrix == null || labelsList == null || labelsList.isEmpty()) {
      log.warn("Labels list is empty!");
      return new ArrayList<>();
    }

    if (!normalizedLabels) {
      synchronized (this) {
        if (!normalizedLabels) {
          labelsMatrix.diviColumnVector(labelsMatrix.norm1(1));
          normalizedLabels = true;
        }
      }
    }

    INDArray similarity = Transforms.unitVec(labelVector).mmul(labelsMatrix.transpose());
    List<Double> highToLowSimList = getTopN(similarity, topN + 20);

    for (Double aHighToLowSimList : highToLowSimList) {
      String word = labelsList.get(aHighToLowSimList.intValue()).getLabel();
      if (word != null && !word.equals("UNK") && !word.equals("STOP")) {
        INDArray otherVec = lookupTable.vector(word);
        double sim = Transforms.cosineSim(labelVector, otherVec);

        result.add(new BasicModelUtils.WordSimilarity(word, sim));
      }
    }

    result.sort(new BasicModelUtils.SimilarityComparator());

    return BasicModelUtils.getLabels(result, topN);
  }

  /**
   * Get top N elements
   *
   * @param vec the vec to extract the top elements from
   * @param N the number of elements to extract
   * @return the indices and the sorted top N elements
   */
  private List<Double> getTopN(INDArray vec, int N) {
    BasicModelUtils.ArrayComparator comparator = new BasicModelUtils.ArrayComparator();
    PriorityQueue<Double[]> queue = new PriorityQueue<>(vec.rows(), comparator);

    for (int j = 0; j < vec.length(); j++) {
      final Double[] pair = new Double[] {vec.getDouble(j), (double) j};
      if (queue.size() < N) {
        queue.add(pair);
      } else {
        Double[] head = queue.peek();
        if (comparator.compare(pair, head) > 0) {
          queue.poll();
          queue.add(pair);
        }
      }
    }

    List<Double> lowToHighSimLst = new ArrayList<>();

    while (!queue.isEmpty()) {
      double ind = queue.poll()[1];
      lowToHighSimLst.add(ind);
    }
    return Lists.reverse(lowToHighSimLst);
  }

  @Override
  public void fit() {
    if (lookupTable == null) {
      super.fit();
    }

    Map<String, INDArray> hvs = getPar2Hier(labelAwareIterator, lookupTable, labelsSource.getLabels(), k, smoothing);
    for (Map.Entry<String, INDArray> entry : hvs.entrySet()) {
      lookupTable.putVector(entry.getKey(), entry.getValue());
    }

    extractLabels();
  }

  @Override
  public INDArray getWordVectorsMean(Collection<String> labels) {
    INDArray array = getWordVectors(labels);
    return array.mean(0);
  }

  @Override
  public INDArray getWordVectors(@NonNull Collection<String> labels) {
    int indexes[] = new int[labels.size()];
    int cnt = 0;
    for (String label : labels) {
      if (vocab.containsWord(label)) {
        indexes[cnt] = vocab.indexOf(label);
      } else
        indexes[cnt] = -1;
      cnt++;
    }

    while (ArrayUtils.contains(indexes, -1)) {
      indexes = ArrayUtils.removeElement(indexes, -1);
    }

    return Nd4j.pullRows(lookupTable.getWeights(), 1, indexes);
  }

  @Override
  public Collection<String> wordsNearest(INDArray words, int top) {
    return modelUtils.wordsNearest(words, top);
  }

  @Override
  public WeightLookupTable<VocabWord> getLookupTable() {
    return this.lookupTable;
  }

  @Override
  public WeightLookupTable lookupTable() {
    return this.lookupTable;
  }

  @Override
  public VocabCache<VocabWord> getVocab() {
    return this.lookupTable.getVocabCache();
  }

  public enum Method {
    CLUSTER,
    SUM
  }

  /**
   * transforms paragraph vectors into hierarchical vectors
   * @param iterator iterator over docs
   * @param lookupTable the paragraph vector table
   * @param labels the labels
   * @param k the no. of centroids
   * @return a map doc->hierarchical vector
   */
  private static Map<String, INDArray> getPar2Hier(IssueIterator iterator,
                                                   WeightLookupTable<VocabWord> lookupTable,
                                                   List<String> labels, int k, Method method) {
    Collections.sort(labels);
    Collection<Issue> issues = iterator.getIssues();

    Map<String, INDArray> hvs = new TreeMap<>();
    // for each doc
    for (Issue issue : issues) {
      getPar2HierVector(lookupTable, issue, k, hvs, method);
    }
    return hvs;
  }

  /**
   * base case: on a leaf hv = pv
   * on a non-leaf node with n children: hv = pv + k centroids of the n hv
   */
  private static void getPar2HierVector(WeightLookupTable<VocabWord> lookupTable, Issue issue,
                                        int k, Map<String, INDArray> hvs, Method method) {
    if (hvs.containsKey(issue.getKey().getValue())) {
      hvs.get(issue.getKey().getValue());
      return;
    }
    INDArray hv = lookupTable.vector(issue.getKey().getValue());

    List<Comment> comments = issue.getComments();
    if (comments.size() == 0) {
      // just the pv
      hvs.put(issue.getKey().getValue(), hv);
    } else {
      INDArray chvs = Nd4j.zeros(comments.size(), hv.columns());
      int i = 0;
      for (Comment desc : comments) {
        // child hierarchical vector
        INDArray chv = lookupTable.vector(desc.getId());
        chvs.putRow(i, chv);
        i++;
      }

      double[][] centroids;
      if (chvs.rows() > k) {
        centroids = getTruncatedVT(chvs, k);
      } else if (chvs.rows() == 1) {
        centroids = getDoubles(chvs.getRow(0));
      } else {
        centroids = getTruncatedVT(chvs, 1);
      }
      switch (method) {
        case CLUSTER:
          INDArray matrix = Nd4j.zeros(centroids.length + 1, hv.columns());
          matrix.putRow(0, hv);
          for (int c = 0; c < centroids.length; c++) {
            matrix.putRow(c + 1, Nd4j.create(centroids[c]));
          }
          hv = Nd4j.create(getTruncatedVT(matrix, 1));
          break;
        case SUM:
          for (double[] centroid : centroids) {
            hv.addi(Nd4j.create(centroid));
          }
          break;
      }

      hvs.put(issue.getKey().getValue(), hv);
    }
  }

  private static double[][] getTruncatedVT(INDArray matrix, int k) {
    double[][] data = getDoubles(matrix);

    SingularValueDecomposition svd = new SingularValueDecomposition(MatrixUtils.createRealMatrix(data));

    double[][] truncatedVT = new double[k][svd.getVT().getColumnDimension()];
    svd.getVT().copySubMatrix(0, k - 1, 0, truncatedVT[0].length - 1, truncatedVT);
    return truncatedVT;
  }

  private static double[][] getDoubles(INDArray matrix) {
    double[][] data = new double[matrix.rows()][matrix.columns()];
    for (int i = 0; i < data.length; i++) {
      for (int j = 0; j < data[0].length; j++) {
        data[i][j] = matrix.getDouble(i, j);
      }
    }
    return data;
  }
}
