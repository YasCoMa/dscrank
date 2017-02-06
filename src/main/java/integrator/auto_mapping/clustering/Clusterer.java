package integrator.auto_mapping.clustering;

/**
 * An interface defining a Clusterer. A Clusterer groups documents into Clusters based on similarity
 * of their content.
 */
public interface Clusterer {
  /** Cluster the provided list of documents. */
  public ClusterList cluster(DocumentList documentList);
}
