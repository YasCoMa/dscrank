package integrator.auto_mapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Solution for Newsle Clustering question from CodeSprint 2012. This class
 * implements clustering of text documents using Cosine or Jaccard distance
 * between the feature vectors of the documents together with k means
 * clustering. The number of clusters is adapted so that the ratio of the
 * intracluster to intercluster distance is below a specified threshold.
 */
public class KmeansText {

  private static boolean debug;
  // number of iterations to use in k means clustering
  private final int kMeansIter;
  // size of document feature vector
  private final int numFeatures;
  // set true to use cosine distance, otherwise Jaccard distance is used
  private final boolean useCosineDistance;
  // threshold used to determine number of clusters k
  private final double clustThreshold;
  // contents of documents (document ID, document title and document contents)
  private String[][] documents;
  // number of documents
  private int numDocuments;
  // encoded document vectors
  private double[][] documentVectors;
  // precalculated document vector norms
  private double[] vectorNorms;
  // clusters specified as document IDs
  private ArrayList<ComparableArrayList> clusters;
  // cluster centroids
  private double[][] centroids;
  // cluster centroid norms
  private double[] centroidNorms;

  /**
   * Instantiate clusterer
   *
   * @param numFeatures
   *          number of features to use in document feature vectors
   * @param kMeansIter
   *          number of iterations to use in k means clustering
   * @param useCosineDistance
   *          set true to use cosine distance, otherwise Jaccard distance will
   *          be used
   * @param clustThreshold
   *          threshold for intracluster to intercluster distance. Used to
   *          determine number of clusters k
   */
  public KmeansText(int numFeatures, int kMeansIter,
      boolean useCosineDistance, double clustThreshold) {
    this.numFeatures = numFeatures;
    this.kMeansIter = kMeansIter;
    this.useCosineDistance = useCosineDistance;
    this.clustThreshold = clustThreshold;
  }

  /**
   * Parse input into document ID, document title and contents
   *
   * @param input
   */
  private void parseInput(String input) {
    StringTokenizer st = new StringTokenizer(input, "{");
    numDocuments = st.countTokens() - 1;
    String record = st.nextToken(); // empty split to left of {
    documents = new String[numDocuments][3];
    Pattern pattern = Pattern
        .compile("'id':(.*),'title':'(.*)','content':'(.*)'");
    for (int i = 0; i < numDocuments; i++) {
      record = st.nextToken();
      Matcher matcher = pattern.matcher(record);
      if (matcher.find()) {
        documents[i][0] = matcher.group(1); // document ID
        documents[i][1] = matcher.group(2); // document title
        documents[i][2] = matcher.group(3); // document contents
      }
    }
  }

  /**
   * Hash word into integer between 0 and numFeatures-1. Used to form document
   * feature vector
   *
   * @param word
   *          String to be hashed
   * @return hashed integer
   */
  private int hashWord(String word) {
    return Math.abs(word.hashCode()) % numFeatures;
  }

  /**
   * Class which extends ArrayList to allow for easy sorting of clusters
   */
  private class ComparableArrayList extends ArrayList<Integer> implements
      Comparable<ComparableArrayList> {

    private static final long serialVersionUID = 1L;

    /**
     * Allows sorting of ArrayList of ArrayList<Integer> by comparing first
     * entry of constituent ArrayList<Integer>
     */
    public int compareTo(ComparableArrayList x) {
      if (this.get(0) > x.get(0)) {
        return 1;
      } else if (this.get(0) < x.get(0)) {
        return -1;
      } else {
        return 0;
      }
    }

  }

  /**
   * Vectorize documents using Term Frequency - Inverse Document Frequency
   */
  private void vectorizeDocuments() {
    long[] docFreq = new long[numFeatures];
    double[][] termFreq = new double[numDocuments][numFeatures];
    documentVectors = new double[numDocuments][numFeatures];
    for (int docIndex = 0; docIndex < numDocuments; docIndex++) {
      String[] words = documents[docIndex][2].split("[^\\w]+");

      // Calculate word histogram for document
      long[] freq = new long[numFeatures];
      for (int i = 0; i < words.length; i++) {
        int hashCode = hashWord(words[i]);
        freq[hashCode]++;
      }

      // Calculate maximum word frequency in document
      long maxFreq = 0;
      for (int i = 0; i < numFeatures; i++) {
        if (freq[i] > maxFreq) {
          maxFreq = freq[i];
        }
      }

      // Normalize word histogram by maximum word frequency
      for (int i = 0; i < numFeatures; i++) {
        if (freq[i] > 0) {
          docFreq[i]++;
        }
        if (maxFreq > 0) {
          termFreq[docIndex][i] = (double) freq[i] / maxFreq;
        }
      }
    }

    // Form document vector using TF-IDF
    for (int i = 0; i < numFeatures; i++) {
      if (docFreq[i] > 0) {
        // Calculate inverse document frequency
        double inverseDocFreq = Math.log((double) numDocuments
            / (double) docFreq[i]);
        // Calculate term frequency inverse document frequency
        for (int docIndex = 0; docIndex < numDocuments; docIndex++) {
          documentVectors[docIndex][i] = termFreq[docIndex][i] * inverseDocFreq;
        }
      }
    }

    // Precalculate norms of document vectors
    vectorNorms = new double[numDocuments];
    for (int docIndex = 0; docIndex < numDocuments; docIndex++) {
      vectorNorms[docIndex] = calcNorm(documentVectors[docIndex]);
      if (useCosineDistance) {
        for (int i = 0; i < numFeatures; i++) {
          documentVectors[docIndex][i] /= vectorNorms[docIndex];
        }
      }
    }

  }

  /**
   * Calculate distance between a document and a cluster's centroid
   *
   * @param docIndex
   *          document index
   * @param clusterIndex
   *          cluster index
   * @return distance between document docIndex and cluster clusterIndex
   */
  private double calcDocClustDistance(int docIndex, int clusterIndex) {
    double[] vector1 = documentVectors[docIndex];
    double[] vector2 = centroids[clusterIndex];
    return calcDistance(vector1, vector2, vectorNorms[docIndex],
        vectorNorms[clusterIndex]);
  }

  /**
   * Calculate distance between two documents
   *
   * @param docIndex1
   *          index of first document
   * @param docIndex2
   *          index of second document
   * @return distance between document docIndex1 and document docIndex2
   */
  private double calcDocDocDistance(int docIndex1, int docIndex2) {
    double[] vector1 = documentVectors[docIndex1];
    double[] vector2 = documentVectors[docIndex2];
    return calcDistance(vector1, vector2, vectorNorms[docIndex1],
        vectorNorms[docIndex2]);
  }

  /**
   * Calculate distance between two vectors
   *
   * @param vector1
   *          first vector
   * @param vector2
   *          second vector
   * @param norm1
   *          precalculated norm of first vector
   * @param norm2
   *          precalculated norm of second vector
   * @return distance between vector1 and vector2
   */
  private double calcDistance(double[] vector1, double[] vector2, double norm1,
      double norm2) {
    if (useCosineDistance) {
      return calcCosineDistance(vector1, vector2, 1.0, 1.0);
    } else {
      return calcJaccardDistance(vector1, vector2, norm1, norm2);
    }
  }

  /**
   * Calculate cosine distance between two vectors
   *
   * @param vector1
   *          first vector
   * @param vector2
   *          second vector
   * @param norm1
   *          precalculated norm of first vector
   * @param norm2
   *          precalculated norm of second vector
   * @return cosine distance between vector1 and vector2
   */
  private double calcCosineDistance(double[] vector1, double[] vector2,
      double norm1, double norm2) {
    double innerProd = 0.0;
    for (int i = 0; i < numFeatures; i++) {
      innerProd += vector1[i] * vector2[i];
    }
    // normalization by norms may be necessary if comparison is done between
    // document and cluster
    return 1.0 - innerProd / norm1 / norm2;
  }

  /**
   * Calculate Jaccard distance between two vectors
   *
   * @param vector1
   *          first vector
   * @param vector2
   *          second vector
   * @param norm1
   *          precalculated norm of first vector
   * @param norm2
   *          precalculated norm of second vector
   * @return Jaccard distance between vector1 and vector2
   */
  private double calcJaccardDistance(double[] vector1, double[] vector2,
      double norm1, double norm2) {
    double innerProd = 0.0;
    for (int i = 0; i < numFeatures; i++) {
      innerProd += vector1[i] * vector2[i];
    }
    return Math.abs(1.0 - innerProd / (norm1 + norm2 - innerProd));
  }

  /**
   * Calculate minimum distance between a document and the existing documents in
   * clusters. Assumes that each cluster has only one document. Used during
   * initialization of k means
   *
   * @param documentIndex
   * @return minimum distance between document documentIndex and documents in
   *         clusters
   */
  private double calcDistanceToExistingClusters(int documentIndex) {
    double distance = Double.MAX_VALUE;
    for (int existingPoint = 0; existingPoint < clusters.size(); existingPoint++) {
      int existingPointIndex = clusters.get(existingPoint).get(0);
      distance = Math.min(distance,
          calcDocDocDistance(documentIndex, existingPointIndex));
    }
    return distance;
  }

  /**
   * Returns norm of a vector
   *
   * @param x
   *          vector
   * @return norm of x
   */
  private double calcNorm(double[] x) {
    double normSquared = 0.0;
    for (int i = 0; i < x.length; i++) {
      normSquared += x[i] * x[i];
    }
    return Math.sqrt(normSquared);
  }

  /**
   * Update centroids and centroidNorms for a specific cluster
   *
   * @param clusterIndex
   *          cluster to update
   */
  private void updateCentroid(int clusterIndex) {
    ComparableArrayList cluster = clusters.get(clusterIndex);
    for (int i = 0; i < numFeatures; i++) {
      centroids[clusterIndex][i] = 0;
      for (int docIndex : cluster) {
        centroids[clusterIndex][i] += documentVectors[docIndex][i];
      }
      centroids[clusterIndex][i] /= cluster.size();
    }
    centroidNorms[clusterIndex] = calcNorm(centroids[clusterIndex]);
  }

  /**
   * Run k mean clustering on document vectors
   *
   * @param k
   *          target number of clusters
   */
  private void runKMeansClustering(int k) {
    clusters = new ArrayList<ComparableArrayList>(k);
    centroids = new double[k][numFeatures];
    centroidNorms = new double[k];
    // marks if a document has already been allocated to a cluster
    boolean[] isAllocated = new boolean[numDocuments];
    // pick initial document for clustering
    Random rnd = new Random(2);
    ComparableArrayList c0 = new ComparableArrayList();
    int rndDocIndex = rnd.nextInt(k);
    // add random document to first cluster
    c0.add(rndDocIndex);
    isAllocated[rndDocIndex] = true;
    clusters.add(c0);
    updateCentroid(0);
    // create new cluster containing furthest document from existing clusters
    for (int clusterIndex = 1; clusterIndex < k; clusterIndex++) {
      // find furthest document
      double furthestDistance = -1;
      int furthestDocIndex = -1;
      for (int candidatePoint = 0; candidatePoint < numDocuments; candidatePoint++) {
        if (!isAllocated[candidatePoint]) {
          double distance = calcDistanceToExistingClusters(candidatePoint);
          if (distance > furthestDistance) {
            furthestDistance = distance;
            furthestDocIndex = candidatePoint;
          }
        }
      }
      ComparableArrayList c = new ComparableArrayList();
      c.add(furthestDocIndex);
      isAllocated[furthestDocIndex] = true;
      clusters.add(c);
      updateCentroid(clusterIndex);
    }

    // process remaining documents
    for (int iter = 0; iter < kMeansIter; iter++) {
      // allocate documents to clusters
      for (int i = 0; i < numDocuments; i++) {
        if (!isAllocated[i]) {
          int nearestCluster = findNearestCluster(i);
          if(nearestCluster!=-1){
        	  clusters.get(nearestCluster).add(i);
          }
        }
      }
      // update centroids and centroidNorms
      for (int i = 0; i < k; i++) {
        updateCentroid(i);
      }
      // prepare for reallocation in next iteration
      if (iter < kMeansIter - 1) {
        for (int i = 0; i < numDocuments; i++) {
          isAllocated[i] = false;
        }
        emptyClusters();
      }

    }

  }

  /**
   * Clear out documents from within each cluster. Used to cleanup at the end of
   * each iteration of k means
   */
  private void emptyClusters() {
    for (int clusterIndex = 0; clusterIndex < clusters.size(); clusterIndex++) {
      clusters.set(clusterIndex, new ComparableArrayList());
    }
  }

  /**
   * Find cluster whose centroid is closest to a document
   *
   * @param docIndex
   * @return cluster closest to docIndex
   */
  private int findNearestCluster(int docIndex) {
    int nearestCluster = -1;
    double nearestDistance = Double.MAX_VALUE;
    for (int clusterIndex = 0; clusterIndex < clusters.size(); clusterIndex++) {
      double docClustDistance = calcDocClustDistance(docIndex, clusterIndex);
      if (docClustDistance < nearestDistance) {
        nearestDistance = docClustDistance;
        nearestCluster = clusterIndex;
      }
    }
    return nearestCluster;
  }

  /**
   * Calculate ratio of average intracluster distance to average intercluster
   * distance. Used to optimize number of clusters k
   *
   * @return ratio of average intracluster distance to average intercluster
   *         distance
   */
  private double calcAvgIntraInterClusterDistance() {
    // calculate average intracluster distance
    double intraDist = 0.0;
    for (int clusterIndex = 0; clusterIndex < clusters.size(); clusterIndex++) {
      ComparableArrayList cluster = clusters.get(clusterIndex);
      for (int docIndex : cluster) {
        intraDist += calcDocClustDistance(docIndex, clusterIndex);
      }
    }
    intraDist /= numDocuments;

    // calculate average intercluster distance
    if (clusters.size() > 1) {
      double interDist = 0.0;
      for (int cluster1Index = 0; cluster1Index < clusters.size(); cluster1Index++) {
        for (int cluster2Index = 0; cluster2Index < clusters.size(); cluster2Index++) {
          if (cluster1Index != cluster2Index) {
            interDist += calcDocClustDistance(cluster1Index, cluster2Index);
          }
        }
      }
      // there are N*N-1 unique pairs of clusters
      interDist /= (clusters.size() * (clusters.size() - 1));
      if (interDist > 0) {
        return intraDist / interDist;
      } else {
        return Double.MAX_VALUE;
      }
    } else {
      return Double.MAX_VALUE;
    }
  }

  /**
   * Run k means clustering on documents in input and evaluate results with
   * expected clustering in expectedOutput
   *
   * @param input
   *          documents to be clustered
   * @param expectedOutput
   *          expected clustering
   */
  public String run(String input) {
    parseInput(input);
    vectorizeDocuments();
    /*
     * increase number of clusters k until ratio of average intracluster
     * distance to intercluster distance is greater than clustThreshold
     */
    for (int k = 1; k <= numDocuments; k++) {
      runKMeansClustering(k);
      double intraInterDistRatio = calcAvgIntraInterClusterDistance();
      if (intraInterDistRatio < clustThreshold) {
        break;
      }
    }
    
    String res = sortAndDisplayClusters();
    return res;
  }

  /**
   * Display clusters in sorted order
   */
  public String sortAndDisplayClusters() {
	String indexes = "";
	String a_original = "[";
	System.out.print("[");
    if(clusters!=null){
    	int max = clusters.get(0).size();
	    for (int i = 0; i < clusters.size(); i++) {
	      Collections.sort(clusters.get(i)); // sort within cluster
	      if(clusters.get(i).size()>max){
	    	  max = clusters.get(i).size();
	      }
	    }
	    Collections.sort(clusters); // sort clusters
	    
	    for (int i = 0; i < clusters.size(); i++) {
	      a_original +=clusters.get(i);
	      System.out.print(clusters.get(i));
	      if (clusters.get(i).size()==max) {
	    	  //try{
		    	  for (int j = 0; j < clusters.get(i).size(); j++) {
		    		  indexes+=clusters.get(i).get(j)+"-";
		    	  }
	    	  //}
	    	  //catch(java.lang.IndexOutOfBoundsException ex){
	    		  
	    	  //}
	      }
	    }
    }
    
    System.out.println("]");
    a_original+="]";
    
    return indexes+"@"+a_original;
  }

  /**
   * Run clustering and profile execution time. If no arguments are supplied
   * input is read from standard input and output is written to standard output
   * If two arguments are supplied then input is read from the file specified by
   * the first argument and output is validated against the file specified by
   * the second input
   *
   */
  public static void main(String[] args) throws IOException {
    String input;
    /*if (args.length > 0) {
      BufferedReader in = new BufferedReader(new FileReader(new File(args[0])));
      BufferedReader out = new BufferedReader(new FileReader(new File(args[1])));
      expectedOutput = out.readLine();
      input = in.readLine();
      debug = true;
    } else {
      Scanner sc = new Scanner(System.in);
      input = sc.nextLine();
      expectedOutput = null;
      debug = false;
    }*/
    
    
    input="[{'uri_target':'http://bio2rdf.org/drugbank:DB03550','id':0,'title':'Isopenicillin N [drugbank:DB03550]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Isopenicillin N [drugbank:DB03550]@en'},{'uri_target':'http://bio2rdf.org/drugbank:DB00417','id':1,'title':'Penicillin V [drugbank:DB00417]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin V is narrow spectrum antibiotic used to treat mild to moderate infections caused by susceptible bacteria. It is a natural penicillin antibiotic that is administered orally. Penicillin V may also be used in some cases as prophylaxis against susceptible organisms. Natural penicillins are considered the drugs of choice for several infections caused by susceptible gram positive aerobic organisms, such as <i>Streptococcus pneumoniae</i>, groups A, B, C and G streptococci, nonenterococcal group D streptococci, viridans group streptococci, and non-penicillinase producing staphylococcus. Aminoglycosides may be added for synergy against group B streptococcus (<i>S. agalactiae</i>), <i>S. viridans</i>, and <i>Enterococcus faecalis</i>. The natural penicillins may also be used as first or second line agents against susceptible gram positive aerobic bacilli such as <i>Bacillus anthracis</i>, <i>Corynebacterium diphtheriae</i>, and <i>Erysipelothrix rhusiopathiae</i>. Natural penicillins have limited activity against gram negative organisms; however, they may be used in some cases to treat infections caused by <i>Neisseria meningitidis</i> and <i>Pasteurella</i>. They are not generally used to treat anaerobic infections. Resistance patterns, susceptibility and treatment guidelines vary across regions.@en'},{'uri_target':'http://bio2rdf.org/drugbank:BE0001304','id':2,'title':'Penicillin acylase [drugbank:BE0001304]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin acylase [drugbank:BE0001304]@en'},{'uri_target':'http://bio2rdf.org/drugbank:BE0003806','id':3,'title':'Penicillin G acylase [drugbank:BE0003806]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin G acylase [drugbank:BE0003806]@en'},{'uri_target':'http://bio2rdf.org/drugbank_resource:Penicillin','id':4,'title':'Penicillin [drugbank_resource:Penicillin]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin [drugbank_resource:Penicillin]@en'}]";
    
    KmeansText clusterer = new KmeansText(10000, 3, true, 0.7);
    clusterer.run(input);
    
  }

}