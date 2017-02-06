package integrator.auto_mapping.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/** Class for storing a collection of documents to be clustered. */
public class DocumentList implements Iterable<Document> {
  private final List<Document> documents = new ArrayList<Document>();
  private int numFeatures;

  /** Construct an empty DocumentList. */
  public DocumentList() {
  }

  /**
   * Construct a DocumentList by parsing the input string. The input string may contain multiple
   * document records. Each record must be delimited by curly braces {}.
   */
  public DocumentList(String input) {
    StringTokenizer st = new StringTokenizer(input, "{");
    int numDocuments = st.countTokens() - 1;
    String record = st.nextToken(); // skip empty split to left of {
    for (int i = 0; i < numDocuments; i++) {
      record = st.nextToken();
      Document document = Document.createDocument(record);
      if (document != null) {
        documents.add(document);
      }
    }
  }

  /** Add a document to the DocumentList. */
  public void add(Document document) {
    documents.add(document);
  }

  /** Clear all documents from the DocumentList. */
  public void clear() {
    documents.clear();
  }

  /** Mark all documents as not being allocated to a cluster. */
  public void clearIsAllocated() {
    for (Document document : documents) {
      document.clearIsAllocated();
    }
  }

  /** Get a particular document from the DocumentList. */
  public Document get(int index) {
    return documents.get(index);
  }

  /** Get the number of features used to encode each document. */
  public int getNumFeatures() {
    return numFeatures;
  }

  /** Determine whether DocumentList is empty. */
  public boolean isEmpty() {
    return documents.isEmpty();
  }

  public Iterator<Document> iterator() {
    return documents.iterator();
  }

  /** Set the number of features used to encode each document. */
  public void setNumFeatures(int numFeatures) {
    this.numFeatures = numFeatures;
  }

  /** Get the number of documents within the DocumentList. */
  public int size() {
    return documents.size();
  }

  /** Sort the documents within the DocumentList by document ID. */
  public void sort() {
    Collections.sort(documents);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Document document : documents) {
        sb.append(document.toString());
    }
    /*for (Document document : documents) {
      sb.append("  ");
      sb.append(document.toString());
      sb.append("\n");
    }*/
    return sb.toString();
  }
}
