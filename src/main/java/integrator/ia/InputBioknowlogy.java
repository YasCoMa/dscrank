package integrator.ia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.vocabulary.RDFS;


public class InputBioknowlogy {
	Set<String> input_concepts = new HashSet<String>();
	
	String sparqlEndpoint = "http://wifo5-03.informatik.uni-mannheim.de/drugbank/sparql";

	String sparqlQuery = ""
			+ "PREFIX isparql:  <java:integrator.ia.isparqlProperties.>"
			+ "PREFIX drugbank:     <http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/drugbank/>"
			+ "PREFIX bio:     <http://www.bioknowlogy.br/ontology/>"
			+ "PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#>"
			+ ""
			+ "SELECT ?uri_source ?label_source ?uri_target ?label_target ?sim_levenshtein ?sim_jaccard_index ?sim_jaro_winkler ?sim_metric_lcs ?sim_ngram ?sim_monge_elkan "
			+ "FROM <data_bioknowlogy.rdf> "
			+ "from named <http://wifo5-04.informatik.uni-mannheim.de/drugbank/all/drugs> "
			+ "WHERE {"
			+ "    ?uri_source  rdfs:label  ?label_source . "
			+ "    { "
			+ "        select ?uri_target ?label_target where { "
			+ "            graph <http://wifo5-04.informatik.uni-mannheim.de/drugbank/all/drugs> { "
			+ "                ?uri_target rdfs:label ?label_target . "
			+ "            } "
			+ "        } "
			+ "    } "
			+ ""
			+ "    ?sim_levenshtein  isparql:levenshtein ( ?label_source ?label_target ) . "
			+ "    ?sim_jaccard_index  isparql:jaccard_index ( ?label_source ?label_target ) . "
			+ "    ?sim_jaro_winkler  isparql:jaro_winkler ( ?label_source ?label_target ) . "
			+ "    ?sim_metric_lcs  isparql:metric_lcs ( ?label_source ?label_target ) . "
			+ "    ?sim_ngram  isparql:ngram ( ?label_source ?label_target ) . "
			+ "    ?sim_monge_elkan  isparql:monge_elkan ( ?label_source ?label_target ) . "
			+ ""
			+ "    filter ( ((?sim_levenshtein + ?sim_jaccard_index + ?sim_jaro_winkler + ?sim_metric_lcs + ?sim_ngram + ?sim_monge_elkan)/6.0)  > 0.40 ) "
			+ "} "
			+ "order by desc (?sim_levenshtein)";
	 
	
	
	public void extract_remote_service (String sparqlEndpoint){
		String sparqlQuery = "select ?s ?p ?o where { ?s ?p ?o . }";
	    @SuppressWarnings("resource")
		QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlEndpoint, sparqlQuery);
	    httpQuery.addParam("output", "json");
	    ResultSet results = httpQuery.execSelect();
	    
	    Model m = ModelFactory.createDefaultModel();
	    //m.setNsPrefix( "bio", "http://www.bioknowlogy.br/ontology/" );
	    //m.setNsPrefix( "res", "http://www.bioknowlogy.br/resource/" );
	   // m.setNsPrefix( "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
	    int cont=0;
	    while (results.hasNext()) {
	    	QuerySolution solution = results.next();
	    	// get the value of the variables in the select clause
	    	String sujeito = solution.get("a").toString();
	    	
	    	// print the output to stdout
	    	Resource s = m.createResource(sujeito);
	    	Property p = RDFS.label;
	    	RDFNode o = null;
	    	o = solution.get("name").asLiteral();
	    	
	    	m.add(s, p, o);
	    	cont++;
	    }
	    
	    try {
	    	OutputStream out = new FileOutputStream("data_publishing_dataset.rdf");
		    m.write( out);
		    out.close();
		} 
	    catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	}
	
	public String translate_term(String term){
		String translated_term=term;
		
		translated_term=translated_term.replace("Classe", "Class");
		translated_term=translated_term.replace("ClasseBetaLactamase", "BetaLactamaseClass");
		translated_term=translated_term.replace("Antibiotico", "Antibiotic");
		translated_term=translated_term.replace("ExperimentoInSilico", "InSilicoExperiment");
		translated_term=translated_term.replace("ExperimentoInVitro", "InVitroExperiment");
		translated_term=translated_term.replace("temTipoResistencia", "hasResistenceType");
		translated_term=translated_term.replace("TipoResistencia", "ResistenceType");
		translated_term=translated_term.replace("Anotacao", "Annotation");
		translated_term=translated_term.replace("pertenceA", "belongsTo");
		translated_term=translated_term.replace("confereResistencia", "ResistentTo");
		translated_term=translated_term.replace("temEntrada", "hasInput");
		translated_term=translated_term.replace("resultadoDe", "isAnnotationof");
		translated_term=translated_term.replace("saidaDe", "isReadof");
		
		return translated_term;
	}
	
	// By endpoint
	public void getInputConcepts(){
		// create the Jena query using the ARQ syntax (has additional support for SPARQL federated queries)
		String sparqlQuery = "select ?a ?b ?c where { ?a <http://www.w3.org/2000/01/rdf-schema#label> ?c . }";
	    Query query = QueryFactory.create(sparqlQuery, Syntax.syntaxARQ) ;
	    
	    @SuppressWarnings("resource")
		QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlEndpoint, query);
	    httpQuery.addParam("output", "json");
	    // execute a Select query
	    ResultSet results = httpQuery.execSelect();
	    
	    Model m = ModelFactory.createDefaultModel();
	    m.setNsPrefix( "bio", "http://www.bioknowlogy.br/ontology/" );
	    m.setNsPrefix( "res", "http://www.bioknowlogy.br/resource/" );
	    m.setNsPrefix( "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
	    while (results.hasNext()) {
	    	QuerySolution solution = results.next();
	    	// get the value of the variables in the select clause
	    	String sujeito = translate_term(solution.get("a").toString());
	    	String predicado = translate_term(solution.get("b").toString());
	    	String objeto = translate_term(solution.get("c").toString());
	    	
	    	System.out.println(sujeito+"-"+predicado+"-"+objeto);
	    	input_concepts.add(sujeito);
	    	input_concepts.add(predicado);
	    	input_concepts.add(objeto);
	    }
	    //return input_concepts;
	}
	
	//By file
	public Set<String> getInputConceptsFromFile(){
		try{
			Model model = ModelFactory.createDefaultModel();
	        model.read("data_bioknowlogy.rdf");
	        StmtIterator iter = model.listStatements();

		    // print out the predicate, subject and object of each statement
		    while (iter.hasNext()) {
		        Statement stmt      = iter.nextStatement();  
		        Resource  subject   = stmt.getSubject(); 
		        Property  predicate = stmt.getPredicate();
		        RDFNode   object    = stmt.getObject();
		        
		        String sujeito = translate_term(subject.getLocalName());
		    	String predicado = translate_term(predicate.getLocalName());
		    	String objeto = "";
		    	if (object instanceof Resource) {
		    		objeto = translate_term(((Resource) object).getLocalName());
		        } 
		        else {
		            objeto = translate_term(object.toString());
		        }
		    	
		    	input_concepts.add(sujeito);
		    	input_concepts.add(predicado);
		    	input_concepts.add(objeto);
		    	
		    } 
	    }
        catch(RiotException  re){
        	System.out.println("texto plano");
        }
		
		return input_concepts;
	}
	
	public static void main(String[] args){
		InputBioknowlogy i = new InputBioknowlogy();
		i.getInputConcepts();
		
		
	}
}
