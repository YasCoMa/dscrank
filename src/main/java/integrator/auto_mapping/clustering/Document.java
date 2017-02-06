package integrator.auto_mapping.clustering;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Class containing an individual document. */
public class Document implements Comparable<Document> {
  private static final Pattern PARSER = Pattern
      .compile("'uri_target':'(.*)','id':(.*),'title':'(.*)','url_origin':'(.*)','content':'(.*)'");
  private final String title;

  private final String contents;
  private final long id;
  private boolean allocated;
  private Vector histogram;
  private Vector vector;
  private int numFeatures;

  /**
   * Construct a document by parsing the provided string into document ID, contents and title. The
   * string must have the format "content": "<content>", "id": "<id>", "title": "<title>". If the
   * provided string has an invalid format then null is returned.
   */
  public static Document createDocument(String record) {
    Document document = null;
    Matcher matcher = PARSER.matcher(record);
    if (matcher.find()) {
      String contents = matcher.group(5);
      long documentID = Long.parseLong(matcher.group(2));
      String title = matcher.group(3);
      document = new Document(documentID, contents, title);
    }
    return document;
  }

  public Document(long id, String contents, String title) {
    this.id = id;
    this.contents = contents;
    this.title = title;
  }

  /** Mark document as not being allocated to a cluster. */
  public void clearIsAllocated() {
    allocated = false;
  }

  /** Allow documents to be sorted by ID. */
  public int compareTo(Document document) {
    if (id > document.getId()) {
      return 1;
    } else if (id < document.getId()) {
      return -1;
    } else {
      return 0;
    }
  }

  /** Get the document contents. */
  public String getContents() {
    return contents;
  }

  /** Get document word histogram. The exact format is determined by the Encoder. */
  public Vector getHistogram() {
    return histogram;
  }

  /** Get the document ID. */
  public long getId() {
    return id;
  }

  /** Get number of features in feature vector. */
  public int getNumFeatures() {
    return numFeatures;
  }

  /**
   * Get feature vector for a document. This is typically a version of the histogram normalized for
   * word frequency. The exact format is determined by the Encoder.
   */
  public Vector getVector() {
    return vector;
  }

  /** Determine whether document has been allocated to a cluster. */
  public boolean isAllocated() {
    return allocated;
  }

  /** Set the word histogram for a document. */
  public void setHistogram(Vector histogram) {
    this.histogram = histogram;
  }

  /** Mark document as having been allocated to a cluster. */
  public void setIsAllocated() {
    allocated = true;
  }

  /**
   * Set the feature vector for a document.
   */
  public void setVector(Vector vector) {
    this.vector = vector;
    this.numFeatures = vector.size();
  }

  @Override
  public String toString() {
    //return "Document: " + id + ", Title: " + title;
    return id+"-";
  }
}
