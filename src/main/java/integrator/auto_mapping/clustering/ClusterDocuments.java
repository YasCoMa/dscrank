package integrator.auto_mapping.clustering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Solution for Newsle Clustering question from CodeSprint 2012. This class implements clustering of
 * text documents using Cosine or Jaccard distance between the feature vectors of the documents
 * together with k means clustering. The number of clusters is adapted so that the ratio of the
 * intracluster to intercluster distance is below a specified threshold.
 */
public class ClusterDocuments {
  private static final int CLUSTERING_ITERATIONS = 3;
  private static final double CLUSTERING_THRESHOLD = 0.7;
  private static final int NUM_FEATURES = 10000;

  /**
   * Cluster the text documents in the provided file. The clustering process consists of parsing and
   * encoding documents, and then using Clusterer with a specific Distance measure.
   */
  public String run(String input){
	  String indexes="";
	  
	  DocumentList documentList = new DocumentList(input);
      Encoder encoder = new TfIdfEncoder(NUM_FEATURES);
      encoder.encode(documentList);
      DistanceMetric distance = new CosineDistance();
      Clusterer clusterer = new KMeansClusterer(distance, CLUSTERING_THRESHOLD, CLUSTERING_ITERATIONS);
      ClusterList clusterList = clusterer.cluster(documentList);
      if(clusterList==null){
    	  indexes="";
      }
      else{
    	  indexes=clusterList.toString();
      }
      return indexes;
      
      // Modificação do método toString de Document, DocumentList e ClusterList.
      // Modificação do padrão de compilação do JSON e dos chunks retornados de valor das tags no matcher pattern em Document
  }
  
  public static void main(String[] args) throws IOException {
    /*if (args.length != 1) {
      System.out.println("Usage: ClusterDocuments <filename>\n");
      System.exit(1);
    }
    BufferedReader in = new BufferedReader(new FileReader(new File(args[0])));
    String input = in.readLine();
    in.close();
    
    DocumentList documentList = new DocumentList("[{'uri_target':'http://bio2rdf.org/drugbank:DB03550','id':0,'title':'Isopenicillin N [drugbank:DB03550]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Isopenicillin N [drugbank:DB03550]@en'},{'uri_target':'http://bio2rdf.org/drugbank:DB00417','id':1,'title':'Penicillin V [drugbank:DB00417]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin V is narrow spectrum antibiotic used to treat mild to moderate infections caused by susceptible bacteria. It is a natural penicillin antibiotic that is administered orally. Penicillin V may also be used in some cases as prophylaxis against susceptible organisms. Natural penicillins are considered the drugs of choice for several infections caused by susceptible gram positive aerobic organisms, such as <i>Streptococcus pneumoniae</i>, groups A, B, C and G streptococci, nonenterococcal group D streptococci, viridans group streptococci, and non-penicillinase producing staphylococcus. Aminoglycosides may be added for synergy against group B streptococcus (<i>S. agalactiae</i>), <i>S. viridans</i>, and <i>Enterococcus faecalis</i>. The natural penicillins may also be used as first or second line agents against susceptible gram positive aerobic bacilli such as <i>Bacillus anthracis</i>, <i>Corynebacterium diphtheriae</i>, and <i>Erysipelothrix rhusiopathiae</i>. Natural penicillins have limited activity against gram negative organisms; however, they may be used in some cases to treat infections caused by <i>Neisseria meningitidis</i> and <i>Pasteurella</i>. They are not generally used to treat anaerobic infections. Resistance patterns, susceptibility and treatment guidelines vary across regions.@en'},{'uri_target':'http://bio2rdf.org/drugbank:BE0001304','id':2,'title':'Penicillin acylase [drugbank:BE0001304]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin acylase [drugbank:BE0001304]@en'},{'uri_target':'http://bio2rdf.org/drugbank:BE0003806','id':3,'title':'Penicillin G acylase [drugbank:BE0003806]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin G acylase [drugbank:BE0003806]@en'},{'uri_target':'http://bio2rdf.org/drugbank_resource:Penicillin','id':4,'title':'Penicillin [drugbank_resource:Penicillin]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin [drugbank_resource:Penicillin]@en'}]");
    //[{'uri_target':'http://bio2rdf.org/drugbank:DB01212','id':0,'title':'Ceftriaxone [drugbank:DB01212]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'A broad-spectrum cephalosporin antibiotic with a very long half-life and high penetrability to meninges, eyes and inner ears. [PubChem]@en'},{'uri_target':'http://bio2rdf.org/drugbank:DB01212','id':1,'title':'Ceftriaxone [drugbank:DB01212]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'A broad-spectrum cephalosporin antibiotic with . [PubChem]@en'}]
    Encoder encoder = new TfIdfEncoder(NUM_FEATURES);
    encoder.encode(documentList);
    DistanceMetric distance = new CosineDistance();
    Clusterer clusterer = new KMeansClusterer(distance, CLUSTERING_THRESHOLD, CLUSTERING_ITERATIONS);
    ClusterList clusterList = clusterer.cluster(documentList);
    System.out.println(clusterList);
    */
    ClusterDocuments cd = new ClusterDocuments();
    System.out.println(cd.run("[{'uri_target':'http://bio2rdf.org/drugbank:DB03550','id':0,'title':'Isopenicillin N [drugbank:DB03550]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Isopenicillin N [drugbank:DB03550]@en'},{'uri_target':'http://bio2rdf.org/drugbank:DB00417','id':1,'title':'Penicillin V [drugbank:DB00417]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin V is narrow spectrum antibiotic used to treat mild to moderate infections caused by susceptible bacteria. It is a natural penicillin antibiotic that is administered orally. Penicillin V may also be used in some cases as prophylaxis against susceptible organisms. Natural penicillins are considered the drugs of choice for several infections caused by susceptible gram positive aerobic organisms, such as <i>Streptococcus pneumoniae</i>, groups A, B, C and G streptococci, nonenterococcal group D streptococci, viridans group streptococci, and non-penicillinase producing staphylococcus. Aminoglycosides may be added for synergy against group B streptococcus (<i>S. agalactiae</i>), <i>S. viridans</i>, and <i>Enterococcus faecalis</i>. The natural penicillins may also be used as first or second line agents against susceptible gram positive aerobic bacilli such as <i>Bacillus anthracis</i>, <i>Corynebacterium diphtheriae</i>, and <i>Erysipelothrix rhusiopathiae</i>. Natural penicillins have limited activity against gram negative organisms; however, they may be used in some cases to treat infections caused by <i>Neisseria meningitidis</i> and <i>Pasteurella</i>. They are not generally used to treat anaerobic infections. Resistance patterns, susceptibility and treatment guidelines vary across regions.@en'},{'uri_target':'http://bio2rdf.org/drugbank:BE0001304','id':2,'title':'Penicillin acylase [drugbank:BE0001304]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin acylase [drugbank:BE0001304]@en'},{'uri_target':'http://bio2rdf.org/drugbank:BE0003806','id':3,'title':'Penicillin G acylase [drugbank:BE0003806]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin G acylase [drugbank:BE0003806]@en'},{'uri_target':'http://bio2rdf.org/drugbank_resource:Penicillin','id':4,'title':'Penicillin [drugbank_resource:Penicillin]@en','url_origin':'http://cu.drugbank.bio2rdf.org/sparql','content':'Penicillin [drugbank_resource:Penicillin]@en'}]"));
  }
}
