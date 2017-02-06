package integrator.auto_mapping.clustering;

import java.util.Random;

/** A Clusterer implementation based on k-means clustering. */
public class KMeansClusterer implements Clusterer {
  private static final Random RANDOM = new Random();
  private final double clusteringThreshold;
  private final int clusteringIterations;
  private final DistanceMetric distance;

  /**
   * Construct a Clusterer.
   * 
   * @param distance the distance metric to use for clustering
   * @param clusteringThreshold the threshold used to determine the number of clusters k
   * @param clusteringIterations the number of iterations to use in k-means clustering
   */
  public KMeansClusterer(DistanceMetric distance, double clusteringThreshold,
      int clusteringIterations) {
    this.distance = distance;
    this.clusteringThreshold = clusteringThreshold;
    this.clusteringIterations = clusteringIterations;
  }

  /**
   * Allocate any unallocated documents in the provided DocumentList to the nearest cluster in the
   * provided ClusterList.
   */
  private void allocatedUnallocatedDocuments(DocumentList documentList, ClusterList clusterList) {
	  
	  for (Document document : documentList) {
      if (!document.isAllocated()) {
        Cluster nearestCluster = clusterList.findNearestCluster(distance, document);
        if(nearestCluster!=null){
        	nearestCluster.add(document);
        }
      }
    }
  }

  /**
   * Run k-means clustering on the provided documentList. Number of clusters k is set to the lowest
   * value that ensures the intracluster to intercluster distance ratio is below
   * clusteringThreshold.
   */
  public ClusterList cluster(DocumentList documentList) {
    ClusterList clusterList = null;
    for (int k = 1; k <= documentList.size(); k++) {
      clusterList = runKMeansClustering(documentList, k);
      if (clusterList.calcIntraInterDistanceRatio(distance) < clusteringThreshold) {
        break;
      }
    }
    return clusterList;
  }

  /** Create a cluster with the unallocated document that is furthest from the existing clusters. */
  private Cluster createClusterFromFurthestDocument(DocumentList documentList,
      ClusterList clusterList) {
    Document furthestDocument = clusterList.findFurthestDocument(distance, documentList);
    Cluster nextCluster = new Cluster(furthestDocument);
    return nextCluster;
  }

  /** Create a cluster with a single randomly seelcted document from the provided DocumentList. */
  private Cluster createClusterWithRandomlySelectedDocument(DocumentList documentList) {
    int rndDocIndex = RANDOM.nextInt(documentList.size());
    Cluster initialCluster = new Cluster(documentList.get(rndDocIndex));
    return initialCluster;
  }

  /** Run k means clustering on the provided DocumentList for a fixed number of clusters k. */
  private ClusterList runKMeansClustering(DocumentList documentList, int k) {
    ClusterList clusterList = new ClusterList();
    documentList.clearIsAllocated();
    clusterList.add(createClusterWithRandomlySelectedDocument(documentList));
    while (clusterList.size() < k) {
      clusterList.add(createClusterFromFurthestDocument(documentList, clusterList));
    }
    for (int iter = 0; iter < clusteringIterations; iter++) {
      allocatedUnallocatedDocuments(documentList, clusterList);
      clusterList.updateCentroids();
      if (iter < clusteringIterations - 1) {
        clusterList.clear();
      }
    }
    return clusterList;
  }
}
