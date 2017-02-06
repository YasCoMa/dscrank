package integrator.auto_mapping.clustering;

/**
 * Implementation of Encoder which uses Term Frequency - Inverse Document Frequency (TF-IDF)
 * encoding.
 */
public class TfIdfEncoder implements Encoder {
  private final int numFeatures;
  private Vector inverseDocumentFrequency;

  /**
   * Construct a term frequency - inverse document frequency encoder. The encoder encodes documents
   * into Vectors with the specified number of features.
   */
  public TfIdfEncoder(int numFeatures) {
    this.numFeatures = numFeatures;
  }

  /**
   * Calculate word histogram for the provided document and store in the histogram field. To ensure
   * a constant size histogram Vector the words are first hashed to an integer between 0 and
   * numFeatures - 1.
   */
  private void calcHistogram(Document document) {
    String[] words = document.getContents().split("[^\\w]+");
    Vector histogram = new Vector(numFeatures);
    for (int i = 0; i < words.length; i++) {
      histogram.increment(hashWord(words[i]));
    }
    document.setHistogram(histogram);
  }

  /** Calculate word histograms for all documents in a DocumentList. */
  private void calcHistogram(DocumentList documentList) {
    for (Document document : documentList) {
      calcHistogram(document);
    }
    documentList.setNumFeatures(numFeatures);
  }

  /**
   * Calculate inverse document frequency for the provided DocumentList. The inverse document
   * frequency for a word i is defined as log(N/Ni) where N is the total number documents and Ni is
   * the number of documents where word i occurs. This method requires that the document histogram
   * for each document has already been calculated.
   */
  private void calcInverseDocumentFrequency(DocumentList documentList) {
    Vector documentFrequency = new Vector(numFeatures);
    for (Document document : documentList) {
      for (int i = 0; i < numFeatures; i++) {
        if (document.getHistogram().get(i) > 0) {
          documentFrequency.increment(i);
        }
      }
    }
    inverseDocumentFrequency = documentFrequency.invert().multiply(documentList.size()).log();
  }

  /**
   * Encode the provided document using Term Frequency - Inverse Document Frequency. This method
   * requires that the inverse document frequency and document word histograms have already been
   * calculated.
   */
  private void encode(Document document) {
    Vector tfidf = document.getHistogram().divide(document.getHistogram().max())
        .multiply(inverseDocumentFrequency);
    document.setVector(tfidf);
  }

  /** Encode all documents within the provided DocumentList. */
  public void encode(DocumentList documentList) {
    calcHistogram(documentList);
    calcInverseDocumentFrequency(documentList);
    for (Document document : documentList) {
      encode(document);
    }
    documentList.setNumFeatures(numFeatures);
  }

  /**
   * Hash word into integer between 0 and numFeatures - 1. Used to form document feature vector.
   */
  private int hashWord(String word) {
    return Math.abs(word.hashCode()) % numFeatures;
  }
}
