package integrator.auto_mapping;

import integrator.auto_mapping.clustering.ClusterDocuments;
import integrator.view.Build_view_step1;
import integrator.view.Build_view_step2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.util.ArrayList;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orsoncharts.util.json.JSONArray;
import com.orsoncharts.util.json.JSONObject;

public class Mapping_clustering {
	public static ObjectMapper mapper = new ObjectMapper();
	
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
				+ " optional { ?uri_target  ?p  ?description . filter contains(str(?p), 'descr') . } "
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
					+ " optional { ?uri_target  ?p  ?description . filter contains(str(?p), 'descr') . } "
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
					+ " optional { ?uri_target  ?p  ?description . filter contains(str(?p), 'descr') . } "
					+ " filter regex(?label_target,'"+concept+"', 'i') . ";
					//+ "}";
		}
		
		// Ajustar o from local e colocar de acordo com o caminho do workspace do usuário
		String sparqlQuery = ""
				+ "PREFIX isparql:  <java:integrator.auto_mapping.isparqlProperties.> "
				+ "PREFIX bio:     <http://www.bioknowlogy.br/ontology/> "
				+ "PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#> "
				+ ""
				+ "SELECT ?description ?uri_source ?label_source ?uri_target ?label_target ?sim_levenshtein ?sim_jaro_winkler ?sim_jaccard_index ?sim_dice ?sim_refined_soundex ?sim_double_metaphone ?sim_monge_elkan "
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
				+ "    ?sim_levenshtein  isparql:levenshtein ( '"+concept+"' ?label_target ?uri_target ) . "
				+ "    ?sim_jaro_winkler  isparql:jaro_winkler ( '"+concept+"' ?label_target ?uri_target ) . "
				+ "    ?sim_jaccard_index  isparql:jaccard_index ( '"+concept+"' ?label_target ?uri_target ) . "
				+ "    ?sim_dice  isparql:dice ( '"+concept+"' ?label_target ?uri_target ) . "
				+ "    ?sim_refined_soundex  isparql:refined_soundex ( '"+concept+"' ?label_target ?uri_target ) . "
				+ "    ?sim_double_metaphone  isparql:double_metaphone ( '"+concept+"' ?label_target ?uri_target ) . "
				+ "    ?sim_monge_elkan  isparql:monge_elkan ( '"+concept+"' ?label_target ?uri_target ) . "
				+ ""
				+ "    filter ( ((?sim_levenshtein + ?sim_jaro_winkler + ?sim_jaccard_index + ?sim_dice + ?sim_refined_soundex + ?sim_double_metaphone + ?sim_monge_elkan)/7.0)  > "+cutoff+" ) "
				+ "} "
				+ "order by desc ((?sim_levenshtein + ?sim_jaro_winkler + ?sim_jaccard_index + ?sim_dice + ?sim_refined_soundex + ?sim_double_metaphone + ?sim_monge_elkan)/7.0) "+lim;
		
		return sparqlQuery;
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Object> extract_remote_data(String path, String sparqlQuery, String url_origin, String concept, String uri, String type, Integer limit, Integer index_url_atual, Integer n_pairs){
		ArrayList<Object> results_ = new ArrayList<Object>();
		
		String previous_target="";
		JSONArray jArry=new JSONArray(); // Para usar no clustering, que será realizado para cada url alvo
		Integer n_pairs_individual=0;
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
					  	if(!solution.get("label_target").toString().equalsIgnoreCase(previous_target)){
						  	previous_target = solution.get("label_target").toString();
						  	
					  		bw.append(url_origin+","+uri.replace(",",";")+","+concept.replace(",",";")+","+solution.get("uri_target").toString().replace(",",";")+","+solution.get("label_target").toString().replace(",",";").replace("\n", "")+","+solution.get("sim_levenshtein").asLiteral().getDouble()+","+ solution.get("sim_jaro_winkler").asLiteral().getDouble()+","+ solution.get("sim_jaccard_index").asLiteral().getDouble()+","+ solution.get("sim_dice").asLiteral().getDouble()+","+ solution.get("sim_refined_soundex").asLiteral().getDouble()+","+ solution.get("sim_double_metaphone").asLiteral().getDouble()+","+ solution.get("sim_monge_elkan").asLiteral().getDouble());
						  	bw.newLine();
						  	
							n_pairs++;
							n_pairs_individual++;
									
						  	Double media_tipo1 = (solution.get("sim_levenshtein").asLiteral().getDouble() + solution.get("sim_jaro_winkler").asLiteral().getDouble())/2.0;
						  	Double media_tipo2 = (solution.get("sim_jaccard_index").asLiteral().getDouble() + solution.get("sim_dice").asLiteral().getDouble())/2.0;
						  	Double media_tipo3 = (solution.get("sim_refined_soundex").asLiteral().getDouble() + solution.get("sim_double_metaphone").asLiteral().getDouble())/2.0;
						  	
						  	File file_ = new File(path+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"temp_data_metric.csv");
							FileWriter fw_ = new FileWriter(file_.getAbsoluteFile(), true);
							BufferedWriter bw_ = new BufferedWriter(fw_);
							bw_.append((media_tipo1)+",MT-1,P"+n_pairs);
							bw_.newLine();
							bw_.append((media_tipo2)+",MT-2,P"+n_pairs);
							bw_.newLine();
							bw_.append((media_tipo3)+",MT-3,P"+n_pairs);
							bw_.newLine();
							bw_.append((solution.get("sim_monge_elkan").asLiteral().getDouble())+",MT-4,P"+n_pairs);
							bw_.newLine();
							bw_.close();
						  	
						  	try{
								JSONObject jObjd=new JSONObject();
								jObjd.put("id", n_pairs_individual-1);
								jObjd.put("title", solution.get("label_target").toString().replace("\n", ""));
								jObjd.put("url_origin", url_origin);
								jObjd.put("uri_target", solution.get("uri_target").toString());
								String content="";
								if(solution.get("description")==null){
									content = solution.get("label_target").toString().replace("\n", "");
								}
								else{
									content = (solution.get("description").toString().equalsIgnoreCase("")) ? solution.get("label_target").toString().replace("\n", "") : solution.get("description").toString().replace("\n", "");
						  		}
								jObjd.put("content", content);
								jArry.add(jObjd);
							}
							catch(Exception ex){
	
						    }
					  	}
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
		catch (Exception e){
			System.out.println(e.getMessage());
		}
		
		// Confirmando com o clustering e recuperando elementos do grupo mais populoso para recuperar novas URIs externas
		execute_clustering(n_pairs_individual, jArry, path, type, limit);
		
		results_.add(n_pairs);
		results_.add(n_pairs_individual);
		
		return results_;
	}
	
	public void execute_clustering(Integer n_pairs, JSONArray jArry, String path, String type, Integer limit){
		if(jArry.size()!=0){
			//KmeansText clusterer = new KmeansText(10000, 3, true, 0.7);
			//String indexes = clusterer.run(jArry.toString().replace("\"", "'"));
			//String a_original = indexes.split("@")[1];
			//indexes = indexes.split("@")[0];
			
			// Novo algoritmo de clustering que faz o mesmo mas está mais documentado e mais fácil de manter e obedece as regras de encapsulamento
			// Fonte do novo algoritmo: https://github.com/cendrillon/text-clustering
			// Somente os elementos do primeiro cluster são retornados pois eles possuem valores mais altos de similaridade
			if(n_pairs>2){
				ClusterDocuments cd = new ClusterDocuments();
				System.out.println(jArry.toString().replace("\"", "'"));
				String indexes = cd.run(jArry.toString().replace("\"", "'").replace("\\/", "/"));
			
				JsonNode r= jsonToNode(jArry.toString());
				String f_index="";
				for(int i=0;i<indexes.split("-").length;i++){
					if(i==indexes.split("-").length-1){
						f_index+=indexes.split("-")[i]+".";
					}
					else if(i==indexes.split("-").length-2){
						f_index+=indexes.split("-")[i]+" e ";
					}
					else{
						f_index+=indexes.split("-")[i]+", ";
					}
				}
				
				Mapping_clustering mc = new Mapping_clustering();
				for (JsonNode ind:r){
					if(indexes.contains(ind.get("id").toString()+"-")){
						mc.mount_query_external(path, ind.get("url_origin").toString(), type, limit, ind.get("uri_target").toString());
					}
				}
				
				try{
					String filename= path+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"log_mapping.txt";
				    PrintWriter pw = new PrintWriter(new FileWriter(filename,true)); //the true will append the new data
				    pw.println("Índices mais similares: "+indexes+"_"+f_index+System.lineSeparator());//appends the string to the file
				    pw.println(jArry.toString().replace("\"", "'"));
				    pw.close();
				}
				catch(IOException ioe){
				    //System.err.println("IOException: " + ioe.getMessage());
				}
			}
		}
	}
	
	public String mount_query_external(String path, String url, String type, Integer limit, String uri_target){
		url=url.replace("\"", "");
		if(!url.startsWith("http://")){
			url="http://"+url;
		}
		uri_target=uri_target.replace("\"", "");
		
		//String path_from_local = new File(path+FileSystems.getDefault().getSeparator()+"data_publishing_dataset.rdf").getAbsolutePath();
		String from_named="";
		String consult_target="";
		String lim ="", internal=" "
				//+ "        select ?uri_target ?label_target where { "
				+ " <"+uri_target+"> <http://www.w3.org/2002/07/owl#sameAs> ?uri_external . ";
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
					+ " <"+uri_target+"> <http://www.w3.org/2002/07/owl#sameAs> ?uri_external . "
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
					+ " <"+uri_target+"> <http://www.w3.org/2002/07/owl#sameAs> ?uri_external . ";
					//+ "}";
		}
		
		// Ajustar o from local e colocar de acordo com o caminho do workspace do usuário
		String sparqlQuery = ""
				+ "PREFIX isparql:  <java:integrator.auto_mapping.isparqlProperties.> "
				+ "PREFIX bio:     <http://www.bioknowlogy.br/ontology/> "
				+ "PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#> "
				+ ""
				+ "SELECT ?uri_external "
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
				+ "} ";
		
		int external_pairs=0;
		
		try {
			System.out.println(sparqlQuery);
			Query query_result = QueryFactory.create(sparqlQuery);
			try{
				QueryExecution qexec_result = QueryExecutionFactory.create(query_result);
				try{
					ResultSet results = qexec_result.execSelect();
				
					File file = new File(path+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"result_external_sameas.csv");
					FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
					BufferedWriter bw = new BufferedWriter(fw);
					while (results.hasNext()) {
					  	QuerySolution solution = results.next();
					  	bw.append(uri_target.replace(",",";")+","+solution.get("uri_external").toString().replace(",",";"));
					  	bw.newLine();
					  	
					  	external_pairs++;
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
		
		return sparqlQuery;
	}
	
	public JsonNode jsonToNode(String json) {
        JsonNode root = null;
	    try {
            root = mapper.readTree(json);
        }
        catch (JsonMappingException e) {
            System.out.println("erro[Árvore de mapeamento vazia]");
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        return root;
	    
	}

	public static void main(String[] args){
		try{
			JSONArray jArry=new JSONArray();
			for (int i=0;i<3;i++){
				JSONObject jObjd=new JSONObject();
				jObjd.put("name", "yas");
				jObjd.put("id", i);
				jArry.add(jObjd);
			}
			System.out.println( jArry.toString().replace("\"", "'"));
	    }
		catch(Exception ex){

	    }
	}
}
