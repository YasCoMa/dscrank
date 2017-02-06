package integrator.auto_mapping;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

import integrator.config.Input_publishing_dataset;
import integrator.dscrawler.LinkController;
import integrator.view.Build_view_step1;

public class Consult_mapping {
	public String mount_query(String path, String url, String type, Integer limit, Double cutoff, String concept, String uri){
		url=url.replace("\"", "");
		if(!url.startsWith("http://")){
			url="http://"+url;
		}
		//String path_from_local = new File(path+FileSystems.getDefault().getSeparator()+"data_publishing_dataset.rdf").getAbsolutePath();
		String from_named="";
		String consult_target="";
		String lim ="", internal=" "
				//+ "        select ?uri_target ?label_target where { "
				+ " ?uri_target <http://www.w3.org/2000/01/rdf-schema#label> ?label_target "
				+ " filter regex(str(?label_target),'"+concept+"', 'i') . " ;
				//+ " } ";
		if(type.equalsIgnoreCase("endpoint")){
			from_named="";
			lim= (!(limit==null)) ? "limit "+limit+" " : "";
			String id="";
			String url_query=url;
			if (url.contains("bio2rdf.org")){
				if(url.contains("cu")){
					id = url.substring(url.indexOf("cu.")+3, url.indexOf(".bio2rdf"));
				}
				else{
					id = url.substring(url.indexOf("://")+3, url.indexOf(".bio2rdf"));
				}
				internal="         graph <http://bio2rdf.org/"+id+"_resource:bio2rdf.dataset."+id+".R3> {"
					//+ "        select ?uri_target ?label_target where { "
					+ "                     ?uri_target <http://www.w3.org/2000/01/rdf-schema#label> ?label_target . "
					+ " filter regex(?label_target,'"+concept+"', 'i') . "
					+ "				    } ";
					//+ "				    } ";
				url_query="http://virtuoso.openlifedata.org/sparql";
			}
			consult_target="service <"+url_query+"> ";
			
		}
		else{
			consult_target="graph <"+url+"> ";
			from_named="from named <"+url+"> ";
			internal=" "
					//+ "select ?uri_target ?label_target where { "
					+ "?uri_target <http://www.w3.org/2000/01/rdf-schema#label> ?label_target . "
					+ " filter regex(?label_target,'"+concept+"', 'i') . ";
					//+ "}";
		}
		
		// Ajustar o from local e colocar de acordo com o caminho do workspace do usuário
		String sparqlQuery = ""
				+ "PREFIX isparql:  <java:integrator.auto_mapping.isparqlProperties.> "
				+ "PREFIX bio:     <http://www.bioknowlogy.br/ontology/> "
				+ "PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#> "
				+ ""
				+ "SELECT ?uri_source ?label_source ?uri_target ?label_target ?sim_levenshtein ?sim_jaccard_index ?sim_jaro_winkler ?sim_metric_lcs ?sim_ngram ?sim_monge_elkan "
				+ "from <file:///"+(path+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()).replace("\\", "/")+"data_publishing_dataset.rdf> "
				+ from_named
				+ "WHERE {"
				//+ "    ?uri_source  rdfs:label  ?label_source . "
				//+ "    { "
				//+ "        select * where { "
				+ "            "+consult_target+" { "
				+					internal
				+ "            } "
				
				//+ "        } "
				//+ "    } "
				+ ""
				+ "    ?sim_levenshtein  isparql:levenshtein ( '"+concept+"' ?label_target ) . "
				+ "    ?sim_jaccard_index  isparql:jaccard_index ( '"+concept+"' ?label_target ) . "
				+ "    ?sim_jaro_winkler  isparql:jaro_winkler ( '"+concept+"' ?label_target ) . "
				+ "    ?sim_metric_lcs  isparql:metric_lcs ( '"+concept+"' ?label_target ) . "
				+ "    ?sim_ngram  isparql:ngram ( '"+concept+"' ?label_target ) . "
				+ "    ?sim_monge_elkan  isparql:monge_elkan ( '"+concept+"' ?label_target ) . "
				+ ""
				+ "    filter ( ((?sim_levenshtein + ?sim_jaccard_index + ?sim_jaro_winkler + ?sim_metric_lcs + ?sim_ngram + ?sim_monge_elkan)/6.0)  > "+cutoff+" ) "
				+ "} "
				+ "order by desc (?sim_levenshtein) "+lim;
		
		return sparqlQuery;
	}
	
	public Integer extract_remote_data(String path, String sparqlQuery, String url_origin, String concept, String uri){
		Integer number_of_pairs=0;
				
		try {
			System.out.println(sparqlQuery);
			Query query_result = QueryFactory.create(sparqlQuery);
			try{
				QueryExecution qexec_result = QueryExecutionFactory.create(query_result);
				try{
					ResultSet results = qexec_result.execSelect();
				
					File file = new File(path+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"result_metrics.csv");
					FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
					BufferedWriter bw = new BufferedWriter(fw);
					while (results.hasNext()) {
					  	QuerySolution solution = results.next();
					  	bw.append(url_origin+","+uri.replace(",",";")+","+concept.replace(",",";")+","+solution.get("uri_target").toString().replace(",",";")+","+solution.get("label_target").toString().replace(",",";")+","+solution.get("sim_levenshtein").asLiteral().getDouble()+","+ solution.get("sim_jaccard_index").asLiteral().getDouble()+","+ solution.get("sim_jaro_winkler").asLiteral().getDouble()+","+ solution.get("sim_metric_lcs").asLiteral().getDouble()+","+ solution.get("sim_ngram").asLiteral().getDouble()+","+ solution.get("sim_monge_elkan").asLiteral().getDouble());
					  	bw.newLine();
					  	number_of_pairs++;
					}
					bw.close();
				}
				catch(org.apache.jena.sparql.resultset.ResultSetException ee1){
					System.out.println("Query with incorrect answer or file with wrong type.");
				}
				catch(HttpException ee3){
					System.out.println("URL could not receiving a query.");
				}
			}
			catch(org.apache.jena.query.QueryExecException ee2){
				System.out.println("URL is invalid and the query cannot be continued.");
			}
		}
		catch(org.apache.http.MalformedChunkCodingException e1){
			System.out.println("URL could not be queried.");
		}
		catch (IOException e){
			
		}
		
		return number_of_pairs;
	}
	
	public void execute_calls_to_urls(String path, Set<String> urls, Set<String> types, Integer limit_, Double cutoff){
		Iterator<String> l_urls = urls.iterator ();
		Iterator<String> l_types = types.iterator ();
		while (l_urls.hasNext() && l_types.hasNext()){
			String url = l_urls.next();
			String type = l_types.next();
			
			LinkController l = new LinkController();
			url=l.get_redirected_url(url);
			
			//String query = mount_query(path, url, type, limit_, cutoff);
			//extract_remote_data(path, query, url);
		}
	}
	
	public static void main(String[] args){
		String path = "C:"+FileSystems.getDefault().getSeparator()+"Users"+FileSystems.getDefault().getSeparator()+"QBEX_PC"+FileSystems.getDefault().getSeparator()+"Copy"+FileSystems.getDefault().getSeparator()+"mestrado"+FileSystems.getDefault().getSeparator()+"first_phase";
		Input_publishing_dataset ir = new Input_publishing_dataset();
		ArrayList<ArrayList<String>> a = ir.get_input_labels(path);
		ArrayList<String> labels = a.get(0);
		ArrayList<String> uris = a.get(1);
		Iterator<String> ur = uris.iterator();
		for (String concept:labels){
			String uri=ur.next();
			String internal=""
					+ "    ?uri_target  rdfs:label  ?label_target . "
					+ " filter regex(?label_target,'"+concept+"', 'i') . "
					+ "				     ";
			String consult_target="service <http://wifo5-03.informatik.uni-mannheim.de/drugbank/sparql>";
			
			String sparqlQuery = ""
					+ "PREFIX isparql:  <java:integrator.auto_mapping.isparqlProperties.> "
					+ "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
					+ "PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#> "
					+ ""
					+ "SELECT * "
					+ "from <file:///"+(path+FileSystems.getDefault().getSeparator()).replace("\\", "/")+"data_publishing_dataset.rdf> "
					+ "WHERE {"
					//+ "    ?uri_source  rdfs:label  ?label_source . "
					//+ "     { "
					+ "			"+consult_target+" { "
					//+ "				select * where {"
					+					internal
					//+ "    			}  "
					+ "			}"
					//+ "		} "
					+ "    ?sim_levenshtein  isparql:levenshtein ( '"+concept+"' ?label_target ) . "
					+ "    ?sim_jaccard_index  isparql:jaccard_index ( '"+concept+"' ?label_target ) . "
					+ "    ?sim_jaro_winkler  isparql:jaro_winkler ( '"+concept+"' ?label_target ) . "
					+ "    ?sim_metric_lcs  isparql:metric_lcs ( '"+concept+"' ?label_target ) . "
					+ "    ?sim_ngram  isparql:ngram ( '"+concept+"' ?label_target ) . "
					+ "    ?sim_monge_elkan  isparql:monge_elkan ( '"+concept+"' ?label_target ) . "
					+ ""
					+ "    filter ( ((?sim_levenshtein + ?sim_jaccard_index + ?sim_jaro_winkler + ?sim_metric_lcs + ?sim_ngram + ?sim_monge_elkan)/6.0)  > 0.5 ) "
					+ "}"
					;
			System.out.println(sparqlQuery);
			Query query_result = QueryFactory.create(sparqlQuery);
			
			try{
				QueryExecution qexec_result = QueryExecutionFactory.create(sparqlQuery);
				try{
					ResultSet results = qexec_result.execSelect();
				
					File file = new File(path+FileSystems.getDefault().getSeparator()+"result_metrics.csv");
					FileWriter fw = new FileWriter(file.getAbsoluteFile(),true);
					BufferedWriter bw = new BufferedWriter(fw);
					while (results.hasNext()) {
					  	QuerySolution solution = results.next();
					  	System.out.println("uri label: "+solution.get("uri_target").toString());
					  	System.out.println("label: "+solution.get("label_target").toString());
					  	System.out.println("Jaccard: "+solution.get("sim_jaccard_index").toString());
					  	//System.out.println("Super: "+solution.get("label_super").toString()+" - "+"Class: "+solution.get("label_class").toString()+" - "+"Under: "+solution.get("label_under").toString());
					  	
					  	bw.append("http://wifo5-03.informatik.uni-mannheim.de/drugbank/sparql,"+uri.replace(",",";")+","+concept.replace(",",";")+","+solution.get("uri_target").toString().replace(",",";")+","+solution.get("label_target").toString().replace(",",";")+","+solution.get("sim_levenshtein").asLiteral().getDouble()+","+ solution.get("sim_jaccard_index").asLiteral().getDouble()+","+ solution.get("sim_jaro_winkler").asLiteral().getDouble()+","+ solution.get("sim_metric_lcs").asLiteral().getDouble()+","+ solution.get("sim_ngram").asLiteral().getDouble()+","+ solution.get("sim_monge_elkan").asLiteral().getDouble());
					  	bw.newLine();
					}
					bw.close();
				}
				catch(org.apache.jena.sparql.resultset.ResultSetException ee1){
					System.out.println("Query with incorrect answer or file with wrong type.");
				}
				catch(HttpException ee3){
					System.out.println("URL could not receiving a query.");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			catch(org.apache.jena.query.QueryExecException ee2){
				System.out.println("URL is invalid and the query cannot be continued.");
			}
		}
	}
}

