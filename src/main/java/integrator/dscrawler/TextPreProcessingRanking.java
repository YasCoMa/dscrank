package integrator.dscrawler;

import info.aduna.iteration.Iterations;
import integrator.config.Input_publishing_dataset;
import integrator.view.Build_view_step1;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.FileSystems;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.openrdf.sail.nativerdf.NativeStore;
import org.tartarus.snowball.ext.PorterStemmer;
import org.unix4j.Unix4j;
import org.unix4j.context.DefaultExecutionContext;
import org.unix4j.context.ExecutionContext;
import org.unix4j.context.ExecutionContextFactory;
import org.unix4j.unix.Grep;
import org.unix4j.unix.Wc;
import org.unix4j.util.FileUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.QueryManager;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import opennlp.tools.cmdline.PerformanceMonitor;
import opennlp.tools.cmdline.postag.POSModelLoader;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class TextPreProcessingRanking {
	final static File outputDir = FileUtil.getUserDir();
    final static ExecutionContextFactory contextFactory = new ExecutionContextFactory() {
        public ExecutionContext createExecutionContext() {
            final DefaultExecutionContext context = new DefaultExecutionContext();
            context.setCurrentDirectory(outputDir);
            return context;
        }
    };
    
	Repository rep;
	RepositoryConnection conn = null;
	static WordNetDatabase database;
	static POSModel model;
	static Set<String> input_data;
	
	public RepositoryConnection get_connection(){
		return conn;
	}
	
	public Set<String> tokenizer(String desc){
		Set<String> tokens = new HashSet<String>();
		try {
			InputStream is = new FileInputStream("nlp/en-token.bin");
			 
			TokenizerModel model = new TokenizerModel(is);
		 
			Tokenizer tokenizer = new TokenizerME(model);
		 
			String tokens_list[] = tokenizer.tokenize(desc);
		 
			for (String a : tokens_list)
				tokens.add(a);
		 
			is.close();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tokens;
	}
	
	public Set<String> remove_stopwords(Set<String> valid_tokens){
		Set<String> clean_list = new HashSet<String>();
		POSTaggerME tagger = new POSTaggerME(model);
		
		// JJ: Adjective | JJR:	Adjective, comparative | JJS: Adjective, superlative
		// NN: Noun, singular or mass | NNS: Noun, plural | NNP: Proper noun, singular | NNPS: Proper noun, plural
		String valid_tags = "NN|NNS|NNP|NNPS|JJ|JJR|JJS|";
		for(String a:valid_tokens){
			@SuppressWarnings("deprecation")
			String tag_token = tagger.tag(a);
			if(tag_token.indexOf("/")!=-1){
				String tag = tag_token.split("/")[1];
				if(valid_tags.contains(tag)){
					clean_list.add(a);
				}
			}
			else{
				clean_list.add(a);
			}
		}
		
		return clean_list;
	}
	
	public Set<String> getRelatedTokens(String wordForm){
		Set<String> new_words = new HashSet<String>();
		
		Synset[] synsets = database.getSynsets(wordForm);	
		//  Display the word forms and definitions for synsets retrieved
		if (synsets.length > 0){
			for (int i = 0; i < synsets.length; i++){
				String[] wordForms = synsets[i].getWordForms();
				for (int j = 0; j < wordForms.length; j++){
					new_words.add(wordForms[j]);
				}
				// get just word forms (synonyms)
			}
		}
		
		return new_words;
	}
	
	public Set<String> remove_punctuation(Set<String> tokens){
		Set<String> clean_list = new HashSet<String>();
		String forbidden_tokens=".|,|:|;|'|!|#|$|%|¢|¬|¨|&|*|(|)|+|=|§|ª|º|°|?|<|>|{|}|[|]|@|\"|||";
		
		for(String tok:tokens){
			String aux="";
			for(int i=0;i<tok.length();i++){
				if(!forbidden_tokens.contains(tok.charAt(i)+"|")){
					aux+=tok.charAt(i);
				}
			}
			clean_list.add(aux);
		}
		
		return clean_list;
	}
	
	public String remove_punctuation(String token){
		String forbidden_tokens=".|,|:|;|'|!|#|$|%|¢|¬|¨|&|*|+|=|§|ª|º|°|?|<|>|{|}|[|]|@|\"|||";
		
		String aux="";
		for(int i=0;i<token.length();i++){
			if(!forbidden_tokens.contains(token.charAt(i)+"|")){
				aux+=token.charAt(i);
			}
		}
		
		return aux;
	}
	
	public Set<String> getStemmedTokens(Set<String> tokens){
		Set<String> stemmed_tokens = new HashSet<String>();
		
		for(String a:tokens){
			PorterStemmer stemmer = new PorterStemmer();
			stemmer.setCurrent(a); //set string you need to stem
			stemmer.stem();  //stem the word
			stemmed_tokens.add(stemmer.getCurrent());
		}
		
		return stemmed_tokens;
	}
	
	public String getStemmedTokens(String tokem){
		
		PorterStemmer stemmer = new PorterStemmer();
		stemmer.setCurrent(tokem); //set string you need to stem
		stemmer.stem();  //stem the word
		String aux = stemmer.getCurrent();
		
		return aux;
	}
	
	public Set<String> treat_text(String desc) {
		Set<String> description_bag_of_words = new HashSet<String>();
		
		Set<String> all_tokens = tokenizer(desc);
		Set<String> valid_tokens = remove_punctuation(all_tokens);
		//Set<String> out_stop = remove_stopwords(valid_tokens);
		
		Set<String> bag_of_words = new HashSet<String>();
		bag_of_words.addAll(valid_tokens);
		for (String a:valid_tokens){
			bag_of_words.addAll(getRelatedTokens(a));
		}
		
		description_bag_of_words.addAll(getStemmedTokens(bag_of_words));
		
		return description_bag_of_words;
	}
	
	public Set<String> treat_word_input(Set<String> word) {
		Set<String> description_bag_of_words = new HashSet<String>();
		
		Set<String> valid_tokens = remove_punctuation(word);
		//Set<String> out_stop = remove_stopwords(valid_tokens);
		
		description_bag_of_words.addAll(getStemmedTokens(valid_tokens));
		
		return description_bag_of_words;
	}
	
	public void init_database(String path){
		
		
		//try {
			//model = new POSModelLoader().load(new File("nlp/en-pos-maxent.bin"));
		/*} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		
		System.setProperty("wordnet.database.dir", "dict/");
		database = WordNetDatabase.getFileInstance();
		
		File file = new File(path+FileSystems.getDefault().getSeparator()+"rdf_database");
		// Enable the construction of a new database to receive the possible new datasets and their respective scores 
		if(file.exists()){
			delete(file);
		}
		
		rep = new SailRepository(new NativeStore(file));
		try {
			rep.initialize();
			
			conn = rep.getConnection();
		}
		catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int test_code(String u){
		int statusCode=0;
		URL url;
		try {
			url = new URL(u);
			HttpURLConnection http = (HttpURLConnection)url.openConnection();
			http.setReadTimeout(30000);
			statusCode = http.getResponseCode();
		} 
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
		}
		catch (Exception e){
			
		}
		
		return statusCode;
	}
	
	// Do the query as a second part of the relevance calculus
	public int enrich_bag_of_concepts(String path, String description, Set<String> urls, Integer limit, int id_){
		// Pull concepts from URLs and verify them against the concepts of the publishing data
		int pre_score=0;
		int file_built=0; 
		
		Iterator<String> iurl = urls.iterator();

		try {
			File file = new File(path+FileSystems.getDefault().getSeparator()+"temp_remote_concepts.txt");
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			if (!file.exists()) {
				file.createNewFile();
			}
			bw.newLine();
			
			long time_spent_endpoint=0;
			
			while(iurl.hasNext()){
				Integer score_current_url=0;
				
				String url=iurl.next();
				LinkController l = new LinkController();
				
				if(!url.contains("bio2rdf.org")){
					url= (l.get_redirected_url(url)!=null) ? l.get_redirected_url(url) : url;
				}
				
				URL s = new URL(url);
				String type = (url.indexOf("sparql")!=-1) ? "endpoint" : "file" ;
				String url_test = (type.equalsIgnoreCase("endpoint")) ? url+"?query=SELECT++*%0AWHERE%0A++%7B+%3Furitarget++%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23label%3E++%3Flabeltarget+%7D%0A%20limit%2010" : url;
				
				if(url.contains("bio2rdf.org")){
					url_test="http://virtuoso.openlifedata.org/sparql"+"?query=SELECT++*%0AWHERE%0A++%7B+%3Furitarget++%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23label%3E++%3Flabeltarget+%7D%0A%20limit%2010";
				}
				int code_test = test_code(url_test);
				
				String lm=Unix4j.grep(Grep.Options.i, url, path+FileSystems.getDefault().getSeparator()+"urls_visited.txt").toStringResult();
				
				if(lm.equalsIgnoreCase("") && !type.equalsIgnoreCase("endpoint") && code_test==200 && (getFileSize(s)!=-1 || (getFileSize(s)>0 && getFileSize(s)<=10000000)) && !url.startsWith("http://www.w3.org/")){
				
					if(!url.startsWith("http://") && !url.startsWith("https://")){
						url="http://"+url;
					}
	
					String from_named="";
					String consult_target="";
					String lim ="";
					String internal=" ?uritarget <http://www.w3.org/2000/01/rdf-schema#label> ?labeltarget . ";
					
					if(type.equalsIgnoreCase("endpoint")){
						consult_target="service <"+url+"> ";
						from_named="";
						lim= (!(limit==null)) ? "limit "+limit+" " : "";
						String id="";
						if (url.contains("bio2rdf.org")){
							if(url.contains("cu")){
								id = url.substring(url.indexOf("cu.")+3, url.indexOf(".bio2rdf"));
							}
							else{
								id = url.substring(url.indexOf("://")+3, url.indexOf(".bio2rdf"));
							}
							internal="         graph <http://bio2rdf.org/"+id+"_resource:bio2rdf.dataset."+id+".R3> {"
								+ "                     ?uritarget <http://www.w3.org/2000/01/rdf-schema#label> ?labeltarget . "
								+ "				    } ";
						}
					}
					else{
						consult_target="graph <"+url+">  ";
						from_named="from named <"+url+"> ";
						internal=" ?uritarget <http://www.w3.org/2000/01/rdf-schema#label> ?labeltarget ";
						lim= (!(limit==null)) ? "limit "+limit+" " : "";
					}
					
					if(type.equalsIgnoreCase("endpoint")){
						if(get_count_concepts(url)>1000000 && limit==null){
							lim=" limit 1000000";
						}
					}
					//Integer offset = (new SecureRandom()).nextInt(10)*100;
					
					String sparqlQuery = ""
							+ "PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#> "
							+ "PREFIX transform:  <java:integrator.dscrawler.simProperty.> "
							+ ""
							+ "SELECT distinct ?labeltarget  "
							+ "from <file:///"+(path+FileSystems.getDefault().getSeparator()).replace("\\", "/")+"data_publishing_dataset.rdf> "
							+ from_named
							+ "WHERE {"
							//+ "    { "
							//+ "        select ?labeltarget where { "
							+ "            "+consult_target+" { "
							+					internal
							+ "    			} "
							//+ "    filter regex(?labeltarget,?label_source_ch, 'i') "
							//+ "        } "
							//+ "    } "
							+ "} "+lim;
					
					System.out.println(sparqlQuery);
					Query query_result = QueryFactory.create(sparqlQuery);
					
					try{
						QueryExecution qexec_result = QueryExecutionFactory.create(query_result);
						//qexec_result.setTimeout(5000);
						try{
							ResultSet results = qexec_result.execSelect();
							while (results.hasNext()) {
								QuerySolution solution = results.next();
								String u_label = solution.get("labeltarget").toString();
								boolean just_number_ob=false;
					        	try{
					        		int a = Integer.parseInt(u_label);
					        	}
					        	catch(Exception e){
					        		just_number_ob=false;
					        	}
					        	
					        	if(!just_number_ob){
								  	bw.write(u_label);
								  	bw.newLine();
					        	}
							  	
							} 	
						}
						catch(org.apache.jena.riot.RiotException ee3){
							System.out.println("Content retrieved could not be read");
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
				else{
					if(lm.equalsIgnoreCase("") && type.equalsIgnoreCase("endpoint") && code_test==200 && !url.startsWith("http://www.w3.org/")){
						long time = System.currentTimeMillis();
						score_current_url=get_count_match_concepts_sparql(url, id_, file_built);
						pre_score+=score_current_url;
						long after = System.currentTimeMillis() - time;
						time_spent_endpoint+=after;
						
						try{
						    String filename= path+FileSystems.getDefault().getSeparator()+"urls_visited.txt";
						    FileWriter uv = new FileWriter(filename,true); //the true will append the new data
						    uv.write(score_current_url+"-"+url+"\n");//appends the string to the file
						    uv.close();
						}
						catch(IOException ioe){
						    //System.err.println("IOException: " + ioe.getMessage());
						}
						
						file_built++;
						
					}
				}
				
				if(!lm.equalsIgnoreCase("")){
					pre_score+=Integer.parseInt(lm.split("-http")[0]);
				}
			}
			
			TextPreProcessingRanking t = new TextPreProcessingRanking();
			System.out.println("Pre-score: "+pre_score+" - Time spent in sparql query: "+t.count_time(time_spent_endpoint));
			
			// Adding the concepts from the description text transformations
			Set<String> bag = treat_text(description);
			Iterator<String> b = bag.iterator();
			while(b.hasNext()){
				bw.write(b.next());
			  	bw.newLine();
			}
			bw.close();
		}
		catch(org.apache.http.MalformedChunkCodingException e1){
			System.out.println("URL could not be queried.");
		}
		catch (IOException e){
			System.out.println("URL could not be queried.");
		}
		
		return pre_score;
	}
	
	public int get_count_match_concepts_fedx(String url){
		int cont=0; 
		
		System.out.println("Querying URL: "+url);
		
		try {
			Config.initialize();
			FedXFactory.initializeSparqlFederation(Arrays.asList(url));
			//887 itens
			String ors=""; int c=0;;
			for (String concept_source:input_data){
				if(!verify_just_number(concept_source)){
					if(c<5){
					ors+="(regex(?labeltarget, \""+concept_source+"\", \"i\")) or ";
					}
				}
				c++;
			}
			ors = ors.substring(0, ors.length()-4);
			System.out.println(ors);
				String internal=" ?uritarget <http://www.w3.org/2000/01/rdf-schema#label> ?labeltarget  . "
						+ "			       filter ("+ors+") . ";
				if (url.contains("bio2rdf.org")){
					String id="";
					if(url.contains("cu")){
						id = url.substring(url.indexOf("cu.")+3, url.indexOf(".bio2rdf"));
					}
					else{
						id = url.substring(url.indexOf("://")+3, url.indexOf(".bio2rdf"));
					}
					internal="         graph <http://bio2rdf.org/"+id+"_resource:bio2rdf.dataset."+id+".R3> {"
						+ "                ?uritarget <http://www.w3.org/2000/01/rdf-schema#label> ?labeltarget . "
						+ "			       filter ("+ors+") . "
						+ "			   } ";
				}
				String q = ""
						+ "SELECT (count(distinct ?labeltarget) AS ?cont)  "
						+ "WHERE {"
						+		internal
						+ "} ";
				System.out.println(q);
					TupleQuery query = QueryManager.prepareTupleQuery(q);
					TupleQueryResult res = query.evaluate();

					while (res.hasNext()) {
						System.out.println(res.next().getValue("cont").stringValue());
						cont+=Integer.parseInt(res.next().getValue("cont").stringValue());
					}
			
			FederationManager.getInstance().shutDown();
			System.out.println("Done.");
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return cont;
	}
	public int get_count_match_concepts_sparql(String url, int id_, int file_built){
		int cont=0, cont_str=0; 
		
		System.out.println("Querying URL: "+url);
		//887 itens
		for (String concept_source:input_data){
			if(!verify_just_number(concept_source)){
				cont_str++;
				System.out.println(concept_source+"-"+cont_str+"/"+input_data.size());
				String internal=" ?uritarget <http://www.w3.org/2000/01/rdf-schema#label> ?labeltarget  . "
						+ "			       filter regex(?labeltarget, \""+concept_source+"\", \"i\") . ";
				String url_query=url;
				if (url.contains("bio2rdf.org")){
					String id="";
					if(url.contains("cu")){
						id = url.substring(url.indexOf("cu.")+3, url.indexOf(".bio2rdf"));
					}
					else{
						id = url.substring(url.indexOf("://")+3, url.indexOf(".bio2rdf"));
					}
					internal="         graph <http://bio2rdf.org/"+id+"_resource:bio2rdf.dataset."+id+".R3> {"
						+ "                ?uritarget <http://www.w3.org/2000/01/rdf-schema#label> ?labeltarget . "
						+ "			       filter regex(?labeltarget, \""+concept_source+"\", \"i\") . "
						+ "			   } ";
					
					url_query="http://virtuoso.openlifedata.org/sparql";
				}
				
				String query = ""
						+ "SELECT (count(distinct ?labeltarget) AS ?cont)  "
						+ "WHERE {"
						+		internal
						+ "} ";
				Query query_result = QueryFactory.create(query);
				
				try{
					QueryEngineHTTP x = (QueryEngineHTTP) QueryExecutionFactory.sparqlService(url_query, query_result);
					x.setTimeout(600000);
					try{
						ResultSet results = x.execSelect();
						int s=0;
						while (results.hasNext()) {
						  	QuerySolution solution = results.next();
						  	int temp_score=solution.get("cont").asLiteral().getInt();
						  	cont+=temp_score;
						  	
						  	if(file_built==0){
							  	try{
							  		String filename= Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"anova_info_ds"+FileSystems.getDefault().getSeparator()+("info_ds"+id_+".txt");
							  		PrintWriter pw = new PrintWriter(new FileWriter(filename,true)); //the true will append the new data
								    pw.print(concept_source+"#"+temp_score+System.getProperty("line.separator"));//appends the string to the file
								    pw.close();
								}
								catch(IOException ioe){
								    //System.err.println("IOException: " + ioe.getMessage());
								}
						  	}
						  	else{
						  		change_info_score_ds(id_, concept_source, temp_score);
						  	}
						  	System.out.println(temp_score);
						  	s+=1;
						}
						if(s==0){
							break;
						}
						System.out.println(s);
					}
					catch(Exception e2){
						break;
					}
				}
				catch(Exception e){
					
				}
				
			}	
		}
		
		return cont;
	}
	
	public void change_info_score_ds(Integer id, String concept_to_search, Integer score){
		String new_content="";
		try {
		    FileReader arq = new FileReader(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"anova_info_ds"+FileSystems.getDefault().getSeparator()+("info_ds"+id+".txt"));
		    BufferedReader lerArq = new BufferedReader(arq);
		    Boolean found=false;
		    String linha = lerArq.readLine(); 
		    while (linha != null) {
		    	String concept = linha.split("#")[0];
		        Integer old_score = Integer.parseInt(linha.split("#")[1]);
		        if(concept.equalsIgnoreCase(concept_to_search)){
		        	linha=concept+"#"+(score+old_score);
		        	found=true;
		        }
		        new_content+=linha+System.getProperty("line.separator");
		        linha = lerArq.readLine(); // lê da segunda até a última linha
		    }

		    arq.close();
		    
		    if(!found){
		    	new_content+=concept_to_search+"#"+(score);
		    }
		    FileOutputStream buffer = null;  
			buffer = new FileOutputStream(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"anova_info_ds"+FileSystems.getDefault().getSeparator()+("info_ds"+id+".txt")); 
			buffer.write(new_content.getBytes());  
			buffer.close();
		}
		catch (IOException e) {
		    System.err.printf("Erro na abertura do arquivo: %s.\n");
		}
		
		
	}
	
	public int get_count_match_concepts(String url){
		int cont=0; 
		
		System.out.println("Querying URL: "+url);
		//887 itens
		for (String concept_source:input_data){
			if(!verify_just_number(concept_source)){		
				String internal=" ?uritarget <http://www.w3.org/2000/01/rdf-schema#label> ?labeltarget  . "
						+ "			       filter regex(?labeltarget, \""+concept_source+"\", \"i\") . ";
				if (url.contains("bio2rdf.org")){
					String id="";
					if(url.contains("cu")){
						id = url.substring(url.indexOf("cu.")+3, url.indexOf(".bio2rdf"));
					}
					else{
						id = url.substring(url.indexOf("://")+3, url.indexOf(".bio2rdf"));
					}
					internal="         graph <http://bio2rdf.org/"+id+"_resource:bio2rdf.dataset."+id+".R3> {"
						+ "                ?uritarget <http://www.w3.org/2000/01/rdf-schema#label> ?labeltarget . "
						+ "			       filter regex(?labeltarget, \""+concept_source+"\", \"i\") . "
						+ "			   } ";
				}
				String query = ""
						+ "SELECT (count(distinct ?labeltarget) AS ?cont)  "
						+ "WHERE {"
						+		internal
						+ "} ";
				
				HttpURLConnection conn;
		        BufferedReader rd;
				try {
					URL url_ = new URL(url+"?query="+URLEncoder.encode(query, "UTF-8")+"&format=text%2Fhtml");
					
					conn = (HttpURLConnection) url_.openConnection();
		            conn.setRequestMethod("GET");
		            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		            String line;
		            
		            while ((line = rd.readLine()) != null) {
		            	if(line.contains("<td")){
		            		try{
		            			cont+=Integer.parseInt(line.substring(line.indexOf("td>")+3, line.indexOf("</td")));
		            		}
		            		catch(Exception e){
		            			//System.out.println("Error on trying to consult URL: "+url+"?query="+URLEncoder.encode(query, "UTF-8")+"&format=text%2Fhtml");
		            		}
		            	}
		            }
		            
				}
				catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					System.out.print("URL is not valid.");
				} 
				catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("HTTP response code was not 200 [ok]");
				}
			}	
		}
		
		return cont;
	}
	
	public boolean verify_just_number(String token){
		boolean just_number=false;
		int cont=0;
		String invalid_ca = "0123456789 ";
		String[] a=invalid_ca.split("");
		for(String y:token.split("")){
			for(String j:a){
				if(y.equalsIgnoreCase(j)){
					cont++;
				}
			}
		}
		if(cont==token.length()){
			just_number=true;
		}
		return just_number;
	}
	
	public int get_count_concepts(String url){
		int cont=0; 
		
		String internal=" ?uritarget <http://www.w3.org/2000/01/rdf-schema#label> ?labeltarget ";
		if (url.contains("bio2rdf.org")){
			String id="";
			if(url.contains("cu")){
				id = url.substring(url.indexOf("cu.")+3, url.indexOf(".bio2rdf"));
			}
			else{
				id = url.substring(url.indexOf("://")+3, url.indexOf(".bio2rdf"));
			}
			internal="         graph <http://bio2rdf.org/"+id+"_resource:bio2rdf.dataset."+id+".R3> {"
				+ "                     ?uritarget <http://www.w3.org/2000/01/rdf-schema#label> ?labeltarget . "
				+ "				    } ";
		}
		String query = ""
				+ "SELECT (count(distinct ?labeltarget) AS ?cont)  "
				+ "WHERE {"
				+		internal
				+ "} ";
		
		Query query_result = QueryFactory.create(query);
		query=query_result.toString();
		
		HttpURLConnection conn;
        BufferedReader rd;
		try {
			URL url_ = new URL(url+"?query="+URLEncoder.encode(query, "UTF-8")+"&format=text%2Fhtml");
			conn = (HttpURLConnection) url_.openConnection();
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            
            while ((line = rd.readLine()) != null) {
            	if(line.contains("<td")){
            		cont=Integer.parseInt(line.substring(line.indexOf("td>")+3, line.indexOf("</td")));
            	}
            }
		}
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			System.out.print("URL is not valid.");
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("HTTP response code was not 200 [ok]");
		}     
		return cont;
	}
	
	public int getFileSize(URL url) {
		HttpURLConnection conn = null;
	    try {
	        conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("HEAD");
	        conn.getInputStream();
	        return conn.getContentLength();
	    } 
	    catch (IOException e) {
	        return -1;
	    } 
	    finally {
	        conn.disconnect();
	    }
    }
	
	public double verify_score(String path, Integer pre_score, int id){
		Integer score=pre_score;
		
		for (String a:input_data){
			if(!verify_just_number(a)){
				final File testFile = new File(path+FileSystems.getDefault().getSeparator()+"temp_remote_concepts.txt");
				Integer sc=Integer.parseInt(Unix4j.use(contextFactory).grep(a.toLowerCase(), testFile).wc(Wc.Options.l).toStringResult());
				score+=sc;
				change_info_score_ds(id, a, sc);
			}
		}
		
		return score;
	}
	
	public ArrayList<Double> calculate_means(){
		ArrayList<Double> means = new ArrayList<Double>();
		File f = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"anova_info_ds");
		File[] fl = f.listFiles();
        for (int i=0; i<fl.length;i++){
        	String fn = fl[i].getAbsolutePath();
        	File a = new File(fn);
        	
        	try {
    		    FileReader arq = new FileReader(a);
    		    BufferedReader lerArq = new BufferedReader(arq);
    		    
    		    Integer sum_score_ds=0;
    		    String linha = lerArq.readLine(); 
    		    while (linha != null) {
    		    	Integer score = Integer.parseInt(linha.split("#")[1]);
    		        sum_score_ds+=score;
    		        
    		        linha = lerArq.readLine(); // lê da segunda até a última linha
    		    }

    		    arq.close();

    		    means.add((double) (sum_score_ds/input_data.size()));
    		}
    		catch (IOException e) {
    		    System.err.printf("Erro na abertura do arquivo: %s.\n");
    		}
        }
		return means;
	}
	
	public ArrayList<Double> calculate_variances(ArrayList<Double> means){
		ArrayList<Double> variances = new ArrayList<Double>();
		File f = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"anova_info_ds");
		File[] fl = f.listFiles();
        for (int i=0; i<fl.length;i++){
        	String fn = fl[i].getAbsolutePath();
        	File a = new File(fn);
        	
        	try {
    		    FileReader arq = new FileReader(a);
    		    BufferedReader lerArq = new BufferedReader(arq);
    		    
    		    Double sum_variance_ds=0.0;
    		    String linha = lerArq.readLine(); 
    		    while (linha != null) {
    		    	Integer score = Integer.parseInt(linha.split("#")[1]);
    		        sum_variance_ds+=Math.pow((score-means.get(i)),2);
    		        
    		        linha = lerArq.readLine(); // lê da segunda até a última linha
    		    }

    		    arq.close();

    		    variances.add(sum_variance_ds);
    		}
    		catch (IOException e) {
    		    System.err.printf("Erro na abertura do arquivo: %s.\n");
    		}
        }
		return variances;
	}
	
	public void analysis_for_ranking(String path, Integer lim){
		TextPreProcessingRanking t = new TextPreProcessingRanking();
		long time = System.currentTimeMillis();
		
		if(conn==null){
			init_database(path);
		}
		Input_publishing_dataset ip = new Input_publishing_dataset();
		input_data = treat_word_input(ip.get_input_concepts(path));
		
		File file = new File(path+FileSystems.getDefault().getSeparator()+"urls_visited.txt");
		// Enable the construction of the file to test the URLs already visited  
		if(file.exists()){
			delete(file);
		}
		try {
			file.createNewFile();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		File file_log_relevance = new File(path+FileSystems.getDefault().getSeparator()+"log_relevance.txt");
		// Enable the construction of the file with datasets' score information
		if(file_log_relevance.exists()){
			delete(file_log_relevance);
		}
		try {
			file_log_relevance.createNewFile();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		File file_anova = new File(path+FileSystems.getDefault().getSeparator()+"anova_info_ds");
		// Enable the construction of the file to test the URLs already visited  
		if(file_anova.exists()){
			delete(file_anova);
		}
		file_anova.mkdir();
		
		File fXmlFile = new File(path+FileSystems.getDefault().getSeparator()+"dataset_information.xml");
		DocumentBuilderFactory datasets = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		int number_of_datasets = 0;
		try {
			dBuilder = datasets.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("dataset");
			
			Build_view_step1.progressBar.setValue(30);
			for (int temp = 0; temp < nList.getLength(); temp++) {
				File info_ds = new File(path+FileSystems.getDefault().getSeparator()+"anova_info_ds"+FileSystems.getDefault().getSeparator()+("info_ds"+(temp+1)+".txt"));
				if(!file_anova.exists()){
					file_anova.mkdir();
				}
				info_ds.createNewFile();
				
				Node nNode = nList.item(temp);
						
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					number_of_datasets++;
					Element eElement = (Element) nNode;
					
					String url = eElement.getElementsByTagName("url").item(0).getTextContent();
					if(!url.equalsIgnoreCase("")){ // This avoid input of datasets with empty valid urls
						String id =  "Dataset_"+eElement.getAttribute("id");
						
						String name = eElement.getElementsByTagName("name").item(0).getTextContent();
						save_result_in_tripleStore(id, "hasName", name, "string");
						
						Set<String> urls = new HashSet<String>();
						
						if(url.indexOf("@")!=-1){
							String[] aux = url.split("@");
							if(aux.length>1){
								for(int i=0; i<aux.length-1;i++){
									urls.add(aux[i]);
									save_result_in_tripleStore(id, "hasURL", aux[i], "string");
								}
							}
							else{
								urls.add(url.replace("@", ""));
							}
						}
						else{
							urls.add(url);
							save_result_in_tripleStore(id, "hasURL", url, "string");
						}
						
						String description = eElement.getElementsByTagName("description").item(0).getTextContent();
						save_result_in_tripleStore(id, "hasDescription", description, "string");
						
						long time_current = System.currentTimeMillis();
						int pre_score=enrich_bag_of_concepts(path, description, urls, lim, Integer.parseInt(eElement.getAttribute("id")));
						double score = verify_score(path, pre_score, Integer.parseInt(eElement.getAttribute("id")));
						long after_current = System.currentTimeMillis() - time_current;
						
						try{
						    String filename= path+FileSystems.getDefault().getSeparator()+"log_relevance.txt";
						    PrintWriter pw = new PrintWriter(new FileWriter(filename,true)); //the true will append the new data
						    pw.println("Dataset: "+name+" - Pre-score: "+pre_score+" - Main score: "+score+" - Spent time: "+t.count_time(after_current)+System.lineSeparator());//appends the string to the file
						    pw.close();
						}
						catch(IOException ioe){
						    //System.err.println("IOException: " + ioe.getMessage());
						}
						
						Build_view_step1.progressBar.setValue(30+(((1+temp)*70)/nList.getLength()));
					}
				}
			}
			
			calculate_scores();
			
			// Anova table data
			// Degrees of freedom
			/*int df1=number_of_datasets-1;
			int df2=(input_data.size()*number_of_datasets)-number_of_datasets;
			
			ArrayList<Double> means = calculate_means();
			double sum_means = 0;
			String means_str="";
			for(double m:means){
				sum_means+=m;
				means_str+=m+"-";
			}
			double overall_mean = sum_means/means.size();
			double ssb = 0;
			for(double m:means){
				ssb+=(input_data.size())*(Math.pow((m-overall_mean), 2));
			}
			
			double sse = 0;
			String variances_str="";
			ArrayList<Double> variances = calculate_variances(means);
			for(double m:variances){
				sse+=m;
				variances_str+=m+"-";
			}
			
			double mean_square1 = ssb/df1;
			double mean_square2 = sse/df2;
			double F = mean_square1/mean_square2;
			
			FileOutputStream buffer = null;  
			buffer = new FileOutputStream(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"anova_analysis_data.txt"); 
			buffer.write(("Degree of freedom 1: "+df1+System.getProperty("line.separator")).getBytes());  
			buffer.write(("Degree of freedom 2: "+df2+System.getProperty("line.separator")).getBytes());  
			buffer.write(("Means of the datasets: "+means_str+System.getProperty("line.separator")).getBytes());  
			buffer.write(("Overall mean: "+overall_mean+System.getProperty("line.separator")).getBytes());  
			buffer.write(("SSB: "+ssb+System.getProperty("line.separator")).getBytes());  
			buffer.write(("Variances: "+variances_str+System.getProperty("line.separator")).getBytes());  
			buffer.write(("SSE: "+sse+System.getProperty("line.separator")).getBytes());  
			buffer.write(("Mean square 1: "+mean_square1+System.getProperty("line.separator")).getBytes());  
			buffer.write(("Mean square 2: "+mean_square2+System.getProperty("line.separator")).getBytes());  
			buffer.write(("F: "+F+System.getProperty("line.separator")).getBytes());  
			buffer.close();
			*/
			
			export_database(path);
			
		} 
		catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		long after = System.currentTimeMillis() - time;
		try{
		    String filename= path+FileSystems.getDefault().getSeparator()+"log_relevance.txt";
		    FileWriter lr = new FileWriter(filename,true); //the true will append the new data
		    lr.write("Total spent time: "+t.count_time(after)+System.lineSeparator());//appends the string to the file
		    lr.close();
		}
		catch(IOException ioe){
		    //System.err.println("IOException: " + ioe.getMessage());
		}
	}
	
	public void calculate_scores(){
		double alfa = 0.5;
		double beta = 0.5;
		ArrayList<Double> sum_tf_idf = calculate_A_sum_tf_idf();
		ArrayList<Double> sum_recall = calculate_B_sum_presence_term();
		
		String sum_tf_idf_str="";
		String recall_str="";
		for (int temp = 0; temp < sum_tf_idf.size(); temp++) {
			sum_tf_idf_str+=sum_tf_idf.get(temp)+"-";
			recall_str+=sum_recall.get(temp)+"-";
			double new_score = (alfa*sum_tf_idf.get(temp))+(beta*sum_recall.get(temp));
			save_result_in_tripleStore("Dataset_"+(temp+1), "hasScore", String.format(Locale.US, "%8f", new_score), "double");
		}
		
		FileOutputStream buffer = null;  
		try {
			buffer = new FileOutputStream(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"new_formula_analysis_data.txt");
			buffer.write(("Alfa: "+alfa+System.getProperty("line.separator")).getBytes());  
			buffer.write(("Beta: "+beta+System.getProperty("line.separator")).getBytes());  
			buffer.write(("Sum of TF-IDF calculus: "+sum_tf_idf_str+System.getProperty("line.separator")).getBytes());  
			buffer.write(("Recall: "+recall_str+System.getProperty("line.separator")).getBytes());
			buffer.close();
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	
	public ArrayList<Double> calculate_A_sum_tf_idf(){
		ArrayList<Double> ds_sum_tf_idf = new ArrayList<Double>();
		
		File f = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"anova_info_ds");
		File[] fl = f.listFiles();
		String path_template = fl[0].getAbsolutePath();
		
    	for (int i=0; i<fl.length;i++){
    		String sep = (FileSystems.getDefault().getSeparator().toString().equalsIgnoreCase("\\")) ? "\\\\" : "/";
            String[] partes = path_template.split(sep);
            String fn="";
            for(int j=0;j<partes.length;j++){
            	if(j==partes.length-1){
            		fn+="info_ds"+(i+1)+".txt";
            	}
            	else{
            		fn+=partes[j]+FileSystems.getDefault().getSeparator().toString();
                }
            }
        	
        	File a = new File(fn);
        	
        	try {
    		    FileReader arq = new FileReader(a);
    		    BufferedReader lerArq = new BufferedReader(arq);
    		    
    		    Double sum_tf_idf=0.0;
    		    String linha = lerArq.readLine(); 
    		    while (linha != null) {
    		    	String candidate_term = linha.split("#")[0];
    		    	Integer score = Integer.parseInt(linha.split("#")[1]);
    		        
    		    	double tf = score;
    		    	int docs_w_t = documents_with_term(candidate_term);
    		    	double idf = Math.log((fl.length+1)/(docs_w_t+1));
    		    	sum_tf_idf += tf*idf;
    		    	
    		        linha = lerArq.readLine(); // lê da segunda até a última linha
    		        
    		    }
    		    arq.close();

    		    ds_sum_tf_idf.add((double) (sum_tf_idf));
    		}
    		catch (IOException e) {
    		    System.err.printf("Erro na abertura do arquivo: %s.\n");
    		}
        }
        
        return ds_sum_tf_idf;
	}
	
	public Integer documents_with_term(String term){
		Integer d_w_t = 0;

		File f = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"anova_info_ds");
		File[] fl = f.listFiles();
		String path_template = fl[0].getAbsolutePath();
		
    	for (int i=0; i<fl.length;i++){
        	String sep = (FileSystems.getDefault().getSeparator().toString().equalsIgnoreCase("\\")) ? "\\\\" : "/";
            String[] partes = path_template.split(sep);
            String fn="";
            for(int j=0;j<partes.length;j++){
            	if(j==partes.length-1){
            		fn+="info_ds"+(i+1)+".txt";
            	}
            	else{
            		fn+=partes[j]+FileSystems.getDefault().getSeparator().toString();
                }
            }
        	
        	File a = new File(fn);
        	
        	try {
    		    FileReader arq = new FileReader(a);
    		    BufferedReader lerArq = new BufferedReader(arq);
    		    
    		    String linha = lerArq.readLine(); 
    		    while (linha != null) {
    		    	String candidate_term = linha.split("#")[0];
    		    	Integer score = Integer.parseInt(linha.split("#")[1]);
    		    	if((term.equalsIgnoreCase(candidate_term)) && (score>0)){
    		    		d_w_t++;
    		    		break;
    		    	}
    		        
    		        linha = lerArq.readLine(); // lê da segunda até a última linha
    		    }

    		    arq.close();

    		}
    		catch (IOException e) {
    		    System.err.printf("Erro na abertura do arquivo: %s.\n");
    		}
        }
        
        return d_w_t;
	}
	
	public ArrayList<Double> calculate_B_sum_presence_term(){
		ArrayList<Double> sum_presence = new ArrayList<Double>();
		File f = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"anova_info_ds");
		File[] fl = f.listFiles();
		String path_template = fl[0].getAbsolutePath();
		
    	for (int i=0; i<fl.length;i++){
    		String sep = (FileSystems.getDefault().getSeparator().toString().equalsIgnoreCase("\\")) ? "\\\\" : "/";
            String[] partes = path_template.split(sep);
            String fn="";
            for(int j=0;j<partes.length;j++){
            	if(j==partes.length-1){
            		fn+="info_ds"+(i+1)+".txt";
            	}
            	else{
            		fn+=partes[j]+FileSystems.getDefault().getSeparator().toString();
                }
            }

        	File a = new File(fn);
        	
        	try {
    		    FileReader arq = new FileReader(a);
    		    BufferedReader lerArq = new BufferedReader(arq);
    		    
    		    Integer sum_score_ds=0, count_lines=0;
    		    String linha = lerArq.readLine(); 
    		    while (linha != null) {
    		    	count_lines++;
    		    	
    		    	Integer score = Integer.parseInt(linha.split("#")[1]);
    		    	if(score>0){
    		    		sum_score_ds++;
    		    	}
    		        
    		        linha = lerArq.readLine(); // lê da segunda até a última linha
    		    }

    		    arq.close();

    		    sum_presence.add( (sum_score_ds/ (double) count_lines));
    		}
    		catch (IOException e) {
    		    System.err.printf("Erro na abertura do arquivo: %s.\n");
    		}
        }
        
        return sum_presence;
	}
	
	public void export_database(String path){
		try{
			RepositoryResult<Statement> statements =  conn.getStatements(null, null, null, true);
			Model model = Iterations.addAll(statements, new LinkedHashModel());
			try {
				OutputStream out = new FileOutputStream(path+FileSystems.getDefault().getSeparator()+"result_ranking.rdf");
				Rio.write(model, out, RDFFormat.TURTLE);
				out.close();
			} 
			catch (RDFHandlerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void save_result_in_tripleStore(String s, String p, String o, String type_o){
			
		String namespace = "http://localhost/DSCrawler/";

		ValueFactory f  = rep.getValueFactory();
		try{
			conn.begin();
			URI subject = f.createURI(namespace, s);
			URI predicate = f.createURI(namespace, p);
			Literal object = null;
			if(type_o.equalsIgnoreCase("string")){
				object = f.createLiteral(o, XMLSchema.STRING);
			}
			if(type_o.equalsIgnoreCase("integer")){
				object = f.createLiteral(o, XMLSchema.INTEGER);
			}
			if(type_o.equalsIgnoreCase("double")){
				object = f.createLiteral(o, XMLSchema.DOUBLE);
			}
			conn.add(subject, predicate, object);
			//RepositoryResult<Statement> statements =  conn.getStatements(null, null, null, true);
			conn.commit();
		}
		catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close_database(){
		try {
			conn.close();
			rep.shutDown();
		} 
		catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void delete(File element) {
	    if (element.isDirectory()) {
	        for(File f: element.listFiles()) {
	            this.delete(f);
	        }
	    }
	    element.delete();
	}
	
	public String count_time(long t_total){
		String time="";
		long t_seconds = (long) (t_total/1000.0);
		if(t_seconds>60){
			long t_minutes = (int) (t_seconds/60);
			t_seconds = t_seconds%60;
			if(t_minutes<60){
				time+=t_minutes+" minute(s)"+( (t_seconds==0) ? "" : "; "+t_seconds+" second(s)" ); 
			}
			else{
				int t_hours = (int) (t_minutes/60);
				t_minutes = t_minutes%60;
				time+=t_hours+" hour(s)"+( (t_minutes==0) ? "" : "; "+t_minutes+" minute(s)" )+( (t_seconds==0) ? "" : "; "+t_seconds+" second(s)" ); 
			}
		}
		else {
			time+=t_seconds+" second(s)"; 
		}
		
		return time;
	}
	
	public static void main(String[] args){
		/*
		System.setProperty("wordnet.database.dir", "dict/");
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		Synset[] synsets = database.getSynsets("drugs");	
		//  Display the word forms and definitions for synsets retrieved
		if (synsets.length > 0){
			for (int i = 0; i < synsets.length; i++){
				String[] wordForms = synsets[i].getWordForms();
				for (int j = 0; j < wordForms.length; j++){
					System.out.println(wordForms[j]);
				}
			}
		}
		
		
		TextPreProcessingRanking t = new TextPreProcessingRanking();
		long time = System.currentTimeMillis();
		t.analysis_for_ranking("");
		long after = System.currentTimeMillis() - time;
		
		System.out.println("Total during of analysis: "+t.count_time(after));
		
		File file = new File("rdf_database");
		Repository rep = new SailRepository(new NativeStore(file));
		try {
			rep.initialize();
			RepositoryConnection conn = rep.getConnection();
			conn.begin();
			String queryString = "prefix dscrawler: <http:/localhost/DSCrawler/> "
					+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
					+ "select ?name ?url "
					+ "where {"
					+ " ?uri dscrawler:hasName ?name . "
					+ " ?uri dscrawler:hasURL ?url . "
					+ "} ";
			TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

			TupleQueryResult result = tupleQuery.evaluate();
			try {
	            while (result.hasNext()) {  // iterate over the result
					BindingSet bindingSet = result.next();
					Value url = bindingSet.getValue("url");
					Value name = bindingSet.getValue("name");
					System.out.println(name.toString().replace("^^<http://www.w3.org/2001/XMLSchema#string>", "")+" - "+url.toString().replace("^^<http://www.w3.org/2001/XMLSchema#string>", ""));
					//model_results.addRow(new Object[]{name, score});
	            }
			}
			finally {
				result.close();
			}
		}
		catch (RepositoryException e){
			
		} 
		catch (QueryEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (MalformedQueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		//model = new POSModelLoader().load(new File("nlp/en-pos-maxent.bin"));
		
		//System.setProperty("wordnet.database.dir", "dict/");
		//database = WordNetDatabase.getFileInstance();
		//model = new POSModelLoader().load(new File("nlp/en-pos-maxent.bin"));
		
		Set<String> urls = new HashSet<String>();
		urls.add("http://sparql.uniprot.org/");
		urls.add("http://wikipathways.bio2rdf.org/sparql");
		String path = "C:"+FileSystems.getDefault().getSeparator()+"Users"+FileSystems.getDefault().getSeparator()+"QBEX_PC"+FileSystems.getDefault().getSeparator()+"OneDrive"+FileSystems.getDefault().getSeparator()+"mestrado"+FileSystems.getDefault().getSeparator()+"first_phase";
		//String path = "C:"+FileSystems.getDefault().getSeparator()+"Users"+FileSystems.getDefault().getSeparator()+"LABDS"+FileSystems.getDefault().getSeparator()+"Documents"+FileSystems.getDefault().getSeparator()+"yas"+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1";
		String description ="drugbank is a repository to store information about drugs, their relation with symptoms and interactions with diseases.";
		
		TextPreProcessingRanking t = new TextPreProcessingRanking();
		
		Input_publishing_dataset ip = new Input_publishing_dataset();
		//input_data = t.treat_word_input(ip.get_input_concepts(path));
		
		
		//System.out.println(t.enrich_bag_of_concepts(path, description, urls, null,0));
		/*long time = System.currentTimeMillis();
		System.out.println(t.get_count_match_concepts_sparql("http://sider.bio2rdf.org"));
		long after = System.currentTimeMillis() - time;
		System.out.println("Total during of relevance analysis: "+t.count_time(after)+" \n");
    	
		LinkController l = new LinkController();
		try {
			System.out.println((t.getFileSize(new URL("https://www.ebi.ac.uk/rdf/services/chembl/sparql?query=SELECT++*%0AWHERE%0A++%7B+%3Furitarget++%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23label%3E++%3Flabeltarget+%7D%0A%20limit%2010"))));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		/*File a = new File(path+FileSystems.getDefault().getSeparator()+"anova_info_ds");
		File[] fl = a.listFiles();
		String path_template = fl[0].getAbsolutePath();
		for (int i=0; i<fl.length;i++){
			String sep = (FileSystems.getDefault().getSeparator().toString().equalsIgnoreCase("\\")) ? "\\\\" : "/";
            String[] partes = path_template.split(sep);
            String fn="";
            for(int j=0;j<partes.length;j++){
            	if(j==partes.length-1){
            		fn+="info_ds"+(i+1)+".txt";
            	}
            	else{
            		fn+=partes[j]+FileSystems.getDefault().getSeparator().toString();
                }
            }
        	File b = new File(fn);
        	System.out.println(b.exists());
		}
		*/
		String recall="0.002967359050445104-0.005934718100890208-0.08011869436201781-0.0-0.002967359050445104-0.0-0.0-0.005934718100890208-0.0-0.0712166172106825-0.0712166172106825-0.0-0.08902077151335312-0.0-0.002967359050445104-0.002967359050445104-0.03857566765578635-0.011869436201780416-0.0-0.0-0.0-0.026706231454005934-0.002967359050445104-0.01483679525222552-0.07418397626112759-0.01483679525222552-0.008902077151335312-0.04154302670623145-0.0-0.02967359050445104-0.0-0.0-0.002967359050445104-0.0-0.0-0.0-0.005934718100890208-0.008902077151335312-0.002967359050445104-0.005934718100890208-0.008902077151335312-0.002967359050445104-0.0830860534124629-0.008902077151335312-0.06528189910979229-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.06528189910979229-0.04451038575667656-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.03857566765578635-0.002967359050445104-0.0-0.002967359050445104-0.0-0.002967359050445104-0.002967359050445104-0.011869436201780416-0.002967359050445104-0.0-0.0-0.002967359050445104-0.0-0.005934718100890208-0.0-0.0-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.002967359050445104-0.005934718100890208-0.002967359050445104-0.002967359050445104-0.005934718100890208-0.002967359050445104-0.005934718100890208-0.002967359050445104-0.0-0.005934718100890208-";
		for (String k:recall.split("-")){
		//	System.out.println(k);
		}
		System.out.println(0.08902077151335312*337);
	}
}
