package integrator.config;

import info.debatty.java.stringsimilarity.MetricLCS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.StringEncoderComparator;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.codec.language.RefinedSoundex;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.openrdf.model.vocabulary.RDFS;
import org.unix4j.Unix4j;
import org.unix4j.context.DefaultExecutionContext;
import org.unix4j.context.ExecutionContext;
import org.unix4j.context.ExecutionContextFactory;
import org.unix4j.unix.Grep;
import org.unix4j.unix.Wc;
import org.unix4j.util.FileUtil;

public class Input_publishing_dataset {
	final static File outputDir = FileUtil.getUserDir();
    final static ExecutionContextFactory contextFactory = new ExecutionContextFactory() {
        public ExecutionContext createExecutionContext() {
            final DefaultExecutionContext context = new DefaultExecutionContext();
            context.setCurrentDirectory(outputDir);
            return context;
        }
    };
    
	public Boolean verify_rdf_uri(String uri){
		try{
			Model model = ModelFactory.createDefaultModel();
	        model.read(uri);
	        return true;
		}
		catch(RiotException e){
			return false;
		}
	}
	
	public Boolean verify_rdf_endpoint(String sparqlEndpoint){
		try{
			String sparqlQuery = "select distinct ?a ?b ?c where { ?a ?b ?c . } limit 1";
		    Query query = QueryFactory.create(sparqlQuery, Syntax.syntaxARQ) ;
		    
		    @SuppressWarnings("resource")
			QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlEndpoint, query);
		    httpQuery.addParam("output", "json");
		    // execute a Select query
		    @SuppressWarnings("unused")
			ResultSet results = httpQuery.execSelect();
		    
	        return true;
		}
		catch(Exception e){
			return false;
		}
	}
	
	public void extract_data_from_endpoint(String sparqlEndpoint, String path_file){
		try{
			String sparqlQuery = "select distinct ?a ?b ?c where { ?a ?b ?c . } ";
		    Query query = QueryFactory.create(sparqlQuery, Syntax.syntaxARQ) ;
		    
		    @SuppressWarnings("resource")
			QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlEndpoint, query);
		    httpQuery.addParam("output", "json");
		    ResultSet results = httpQuery.execSelect();
		    Model m= ModelFactory.createDefaultModel();
		    while (results.hasNext()) {
		    	QuerySolution solution = results.next();
		    	
		    	String sujeito = solution.get("a").toString();
		    	String predicado = solution.get("b").toString();
		    	RDFNode objeto = solution.get("c");
		    	if(objeto instanceof Resource){
		    		objeto= solution.get("c").asResource();
		    	}
		    	else{
		    		objeto= solution.get("c").asLiteral();
		    	}
		    	
		    	m.add(m.createResource(sujeito), m.createProperty(predicado), objeto);
		    }
		    
		    OutputStream out = new FileOutputStream(path_file);
		    m.write( out);
		    out.close();
	        
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void extract_data_from_file_uri(String address, String path_file){
		try{
			Model model = RDFDataMgr.loadModel(address);
	        OutputStream out = new FileOutputStream(path_file);
		    model.write(out);
		    out.close();
		}
		catch (IOException e){
			
		}
		catch(RiotException e){
			
		}
	}
	
	public String translate_term(String term){
		String translated_term=term;
		
		translated_term=translated_term.replace("Classe", "Class");
		translated_term=translated_term.replace("ClasseBetaLactamase", "BetaLactamaseClass");
		translated_term=translated_term.replace("Antibiotico", "Antibiotic");
		translated_term=translated_term.replace("ExperimentoInSilico", "InSilicoExperiment");
		translated_term=translated_term.replace("ExperimentoInVitro", "InVitroExperiment");
		translated_term=translated_term.replace("temTipoResistencia", "hasResistanceType");
		translated_term=translated_term.replace("TipoResistencia", "ResistanceType");
		translated_term=translated_term.replace("Anotacao", "Annotation");
		translated_term=translated_term.replace("pertenceA", "belongsTo");
		translated_term=translated_term.replace("confereResistencia", "ResistentTo");
		translated_term=translated_term.replace("temEntrada", "hasInput");
		translated_term=translated_term.replace("resultadoDe", "isAnnotationof");
		translated_term=translated_term.replace("saidaDe", "isReadof");
		
		return translated_term;
	}
	
	public Set<String> get_input_concepts(String path){
		Set<String> input_concepts = new HashSet<String>();

		try{
			Model model = RDFDataMgr.loadModel(path+FileSystems.getDefault().getSeparator()+"data_publishing_dataset.rdf");
	        StmtIterator st = model.listStatements();
	        while(st.hasNext()){
	        	Statement s = st.next();
	        	
	        	String subject = translate_term(s.getSubject().getLocalName());
	        	//input_concepts.add(translate_term(s.getPredicate().getLocalName()));
	        	String object="";
	        	if(s.getObject().isResource() ){
	        		object=s.getObject().asResource().getLocalName();
	        	}
	        	else{
	        		object=s.getObject().asLiteral().toString();
	        	}
	        	
	        	boolean just_number_sub=true;
	        	try{
	        		int a = Integer.parseInt(subject);
	        	}
	        	catch(Exception e){
	        		just_number_sub=false;
	        	}
	        	
	        	boolean just_number_ob=false;
	        	try{
	        		int a = Integer.parseInt(object);
	        	}
	        	catch(Exception e){
	        		just_number_ob=false;
	        	}
	        	
	        	if(!just_number_sub)
	        		input_concepts.add(translate_term(subject));
	        	
	        	if(!just_number_ob)
		        	input_concepts.add(translate_term(object));
	        	
		    }
		}
		catch(RiotException e){
			
		}
		
		return input_concepts;
	}
	
	public ArrayList<ArrayList<String>> get_input_labels(String path){
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		ArrayList<String> input_concepts = new ArrayList<String>();
		ArrayList<String> input_uris = new ArrayList<String>();
		
		try{
			Model model = RDFDataMgr.loadModel(path+FileSystems.getDefault().getSeparator()+"data_publishing_dataset.rdf");
	        StmtIterator st = model.listStatements();
	        while(st.hasNext()){
	        	Statement s = st.next();
	        	
	        	String object="";
	        	if(s.getPredicate().getURI().equalsIgnoreCase(RDFS.LABEL.toString())){
		        	if(!input_uris.contains(s.getSubject().toString())){
		        		input_uris.add(s.getSubject().toString());
		        		if(s.getObject().isResource() ){
			        		object=s.getObject().asResource().getLocalName();
			        	}
			        	else{
			        		object=s.getObject().asLiteral().toString();
			        	}
			        	
			        	boolean just_number_ob=false;
			        	try{
			        		int a = Integer.parseInt(object);
			        	}
			        	catch(Exception e){
			        		just_number_ob=false;
			        	}
			        	
			        	if(!just_number_ob)
				        	input_concepts.add(translate_term(object));
		        	}
	        	}
		    }
		}
		catch(RiotException e){
			
		}
		result.add(input_concepts);
		result.add(input_uris);
		
		return result;
	}
	
	public static void main(String[] args){
		try{
			//Model model = RDFDataMgr.loadModel("C:"+FileSystems.getDefault().getSeparator()+"Users"+FileSystems.getDefault().getSeparator()+"LABDS"+FileSystems.getDefault().getSeparator()+"Documents"+FileSystems.getDefault().getSeparator()+"yas"+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"data_publishing_dataset.rdf");
	        //model.write(System.out, "TURTLE");
			//final File testFile = new File("teste_grep.txt" );
			//System.out.println(Unix4j.use(contextFactory).grep("dru", testFile).wc(Wc.Options.l).toStringResult());
	        
		}
		catch(RiotException e){
			
		}
		//Input_publishing_dataset i = new Input_publishing_dataset();
		//String path = "C:"+FileSystems.getDefault().getSeparator()+"Users"+FileSystems.getDefault().getSeparator()+"QBEX_PC"+FileSystems.getDefault().getSeparator()+"Copy"+FileSystems.getDefault().getSeparator()+"mestrado"+FileSystems.getDefault().getSeparator()+"first_phase";
		//System.out.println(i.get_input_concepts(path).size());
		
		RefinedSoundex sndx = new RefinedSoundex();
		DoubleMetaphone meta = new DoubleMetaphone();
	    StringEncoderComparator comparator1 = new StringEncoderComparator(meta);
	    Double sim=(double) comparator1.compare("cloxacillin","penicillin");
	    if(sim<0){
	    	sim=sim*(-1);
	    }
	    String maior="cloxacillin";
	    if("penicillin [clinicaltrials_resource:8cafbdfc254b2d85673d392f58f8a133]".length()>maior.length()){
	    	maior="penicillin [clinicaltrials_resource:8cafbdfc254b2d85673d392f58f8a133]";
	    }
	    MetricLCS lcss = new MetricLCS();
		System.out.println(sim+"-"+lcss.distance(meta.encode("cloxacillin"),meta.encode("cloxacillin"))+"-"+sndx.encode("cloxacillin")+"-"+sndx.encode("flucloxacillin"));
	}
}
