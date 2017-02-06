package integrator.auto_mapping.clustering;

/**
 * Abstract class for measuring distances between documents, clusters and clusterLists. Here
 * distance determines the similarity of objects. The more similar two objects the smaller the
 * distance between them.
 */
public abstract class DistanceMetric {
  /** Calculate the distance between two clusters by comparing their centroids. */
  public double calcDistance(Cluster cluster1, Cluster cluster2) {
    return calcDistance(cluster1.getCentroid(), cluster2.getCentroid());
  }

  /** Calculate the distance between a document and a cluster. */
  public double calcDistance(Document document, Cluster cluster) {
    return calcDistance(document.getVector(), cluster.getCentroid());
  }

  /**
   * Calculate the minimum distance between a document and the centroids of the clusters within a
   * clusterList.
   */
  public double calcDistance(Document document, ClusterList clusterList) {
    double distance = Double.MAX_VALUE;
    for (Cluster cluster : clusterList) {
      distance = Math.min(distance, calcDistance(document, cluster));
    }
    return distance;
  }

  /** Calculate distance between two Vectors. */
  protected abstract double calcDistance(Vector vector1, Vector vector2);
}
