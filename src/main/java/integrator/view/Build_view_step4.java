package integrator.view;

import info.aduna.iteration.Iterations;
import integrator.dscrawler.TextPreProcessingRanking;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.openrdf.sail.nativerdf.NativeStore;
import org.unix4j.Unix4j;
import org.unix4j.unix.Grep;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;

public class Build_view_step4 {
	public static DefaultComboBoxModel<String> model_combo_subs;
	public static JComboBox<String> submis;
	
	private JTextField num_pairs_;
	private JTextField num_groups_;
	private JTextField perf_match;
	private JTextField relate;
	private JTextField not_relate;
	private JLabel notice_fusion;
	private JButton fuse;
	private JTextArea log_info_st4 = new JTextArea();
	
	private static Integer id_submission;
	
	public JPanel join_all_sections(){
		JPanel merge_panels = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		JPanel area_analysis = mount_area_analysis();
		merge_panels.add(area_analysis);
		
		return merge_panels;
	}
	
	public JPanel mount_area_analysis(){
		JPanel analytics = new JPanel(new BorderLayout());
		
		JPanel area_analytics_crowd = new JPanel(new GridLayout(6,1));
		
		JPanel jp_combo_submissions = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel i_combo_subs = new JLabel("Last Submissions: ");
		model_combo_subs = new DefaultComboBoxModel<String>(populate_combo_submissions("silent"));
		submis = new JComboBox<String>(model_combo_subs);
		submis.addItemListener(new ItemChangeListener());
		jp_combo_submissions.add(i_combo_subs);
		jp_combo_submissions.add(submis);
		area_analytics_crowd.add(jp_combo_submissions);
		
		JPanel jp_n_pairs = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel i_num_pairs = new JLabel("Number of pairs: ");
		num_pairs_ = new JTextField(10);
		num_pairs_.setEditable(false);
		jp_n_pairs.add(i_num_pairs);
		jp_n_pairs.add(num_pairs_);
		area_analytics_crowd.add(jp_n_pairs);
		
		JPanel jp_n_groups = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel i_num_groups = new JLabel("Number of groups: ");
		num_groups_ = new JTextField(10);
		num_groups_.setEditable(false);
		jp_n_groups.add(i_num_groups);
		jp_n_groups.add(num_groups_);
		area_analytics_crowd.add(jp_n_groups);
		
		JPanel jp_lab_res = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel i_res = new JLabel("Ready Results: ");
		jp_lab_res.add(i_res);
		area_analytics_crowd.add(jp_lab_res);
		
		JPanel jp_res_detail = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel i_pm = new JLabel("P.M.:");
		perf_match = new JTextField("0");
		perf_match.setEditable(false);
		jp_res_detail.add(i_pm);
		jp_res_detail.add(perf_match);
		
		JLabel i_rel = new JLabel("Rel.:");
		relate = new JTextField("0");
		relate.setEditable(false);
		jp_res_detail.add(i_rel);
		jp_res_detail.add(relate);
		
		JLabel i_no_rel = new JLabel("No-Rel.:");
		not_relate = new JTextField("0");
		not_relate.setEditable(false);
		jp_res_detail.add(i_no_rel);
		jp_res_detail.add(not_relate);
		
		area_analytics_crowd.add(jp_res_detail);
		
		JPanel action_fuse = new JPanel(new FlowLayout(FlowLayout.LEFT));
        notice_fusion = new JLabel("");
		fuse = new JButton("Start fusion");
        fuse.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	Thread_for_fusion tf = new Thread_for_fusion();
	            	tf.start();
	            }
		    }
		);
        fuse.setEnabled(false);
        action_fuse.add(notice_fusion);
        action_fuse.add(fuse);
        area_analytics_crowd.add(action_fuse);
		
        log_info_st4.setEditable(false);
		JScrollPane p_log = new JScrollPane(log_info_st4);
		p_log.setPreferredSize(new Dimension(600,250));
		p_log.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		p_log.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		analytics.add("North", area_analytics_crowd);
		analytics.add("South", p_log);
		
		return analytics;
	}
	
	class Thread_for_fusion extends Thread{  
    	public void run(){  
    		do_fusion();  
    	}  
    }
	
	public void do_fusion(){
		fuse.setEnabled(false);
		submis.setEnabled(false);
		
		// Bring original data to db - ok
		log_info_st4.setText("Retrieving original source dataset ...\n");
		load_source_ds_to_db();
		
		// Calculate how many different resources was considered PM (Junto com a estimativa feita quando carrega os dados de submissão) - Ok
		
		log_info_st4.append("Resolving dependences and executing fusion ...\n");
		Repository rep;
		RepositoryConnection conn;
		
		TextPreProcessingRanking t = new TextPreProcessingRanking();
		if(t.get_connection()!=null){
			try {
				t.get_connection().close();
			} 
			catch (RepositoryException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		File file = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_4"+FileSystems.getDefault().getSeparator().toString()+"db_auxiliar_fusion");
		rep = new SailRepository(new NativeStore(file));
		try{
			long time = System.currentTimeMillis();
			
			rep.initialize();
			ValueFactory f  = rep.getValueFactory();
			conn = rep.getConnection();
			conn.begin();
			
			String filename=Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_4"+FileSystems.getDefault().getSeparator()+("count_targets_approved.txt");
			Build_view_step3 b3=new Build_view_step3();
    	    String urlParameters = "func=18&id_submission="+Build_view_step4.id_submission;
    	    JsonNode results = b3.jsonToNode(b3.post(Build_view_step3.url_api,urlParameters));
    	    
    	    Integer cont_substitutions=0, agreg_perf_match=0, agreg_perf_match_class=0, agreg_relations=0, agreg_relations_diff_est=0, not_related=0;
    	    
			for(JsonNode r:results){
				Boolean substitution = false;
				String final_answer = "";
				
				int pm = r.get("cont_pm").asInt();
    	    	int rel = r.get("cont_r").asInt();
    	    	int norel = r.get("cont_nr").asInt();
    	    	
    	    	String source_type = r.get("type_source").asText();
    	    	String target_type = r.get("type_target").asText();
    	    		
    	    	if((pm>rel) && (pm>norel)){
    	    		final_answer="pm";
    	    		String lm=Unix4j.grep(Grep.Options.i, r.get("uri_source").asText(), filename).toStringResult();
    				int cont_resources = Integer.parseInt(lm.split("\\%\\$\\@")[1]);
    				
    				if(source_type.equalsIgnoreCase(target_type)){
	    				if(cont_resources==1){
	    					substitution = true;
	    					cont_substitutions++;
	    				}
	    				else{
	    					if(source_type.equalsIgnoreCase("resource")){
	    						agreg_perf_match++;
	    					}
	    					else{
	    						agreg_perf_match_class++;
	    					}
	    				}
    				}
    				else{
    					agreg_relations_diff_est++;
    					final_answer="rel";
    				}
    	    	}
    	    	if(((rel>pm) && (rel>norel)) || ((rel==pm) && ((rel+pm)>norel))){
    	    		final_answer="rel";
    	    		agreg_relations++;
    	    	}
    	    	if((norel>pm) && (norel>rel)){
    	    		final_answer="no-rel"; // Nothing to do
    	    		not_related++;
    	    	}
    	    	
    	    	if(substitution){
    	    		String queryString = ""
    						+ " delete {"
    						+ " <"+r.get("uri_source").asText()+"> ?p ?o "
    						+ "} "
    						+ " insert { "
    						+ " <"+r.get("uri_target").asText()+"> ?p ?o "
    						+ "} "
    						+ "where {"
    						+ " <"+r.get("uri_source").asText()+"> ?p ?o "
    						+ "} ";
    	    		Update update = conn.prepareUpdate(QueryLanguage.SPARQL, queryString);
    	            update.execute(); 
    	            
    	           queryString = ""
    						+ " delete {"
    						+ " ?s ?p <"+r.get("uri_source").asText()+"> "
    						+ "} "
    						+ " insert { "
    						+ " ?s ?p <"+r.get("uri_target").asText()+"> "
    						+ "} "
    						+ "where {"
    						+ " ?s ?p <"+r.get("uri_source").asText()+"> "
    						+ "} ";
    	    		update = conn.prepareUpdate(QueryLanguage.SPARQL, queryString);
    	            update.execute(); 
    				/*
    				queryString = ""
    						+ " insert { "
    						+ " ?s ?p ?o2 "
    						+ "} "
    						+ "where {"
    						+ " ?s ?p ?o . "
    						+ " FILTER(REGEX(?o, '"+r.get("uri_source").asText()+"', 'i')) "
    						+ " BIND(REPLACE(?o, '"+r.get("uri_source").asText()+"', '"+r.get("uri_target").asText()+"', 'i') AS ?o2) "
    						+ "} ";
    				update = conn.prepareUpdate(QueryLanguage.SPARQL, queryString);
    	            update.execute(); 
    				*/
    	    	}
    	    	else{
    	    		if(!final_answer.equalsIgnoreCase("no-rel")){
	    	    		URI subject = f.createURI(r.get("uri_source").asText());
	    	    		URI object = f.createURI(r.get("uri_target").asText());
	    	    		URI predicate = null;
	    	    		if(final_answer.equalsIgnoreCase("rel")){
		    	    		predicate = f.createURI(RDFS.seeAlso.getURI());
		    			}
		    			if(final_answer.equalsIgnoreCase("pm")){
		    				if(source_type.equalsIgnoreCase("resource")){
		    					predicate = f.createURI(OWL.sameAs.getURI());
		    				}
		    				else{
		    					predicate = f.createURI(OWL.equivalentClass.getURI());
		    				}
		    			}
		    			conn.add(subject, predicate, object);
    	    		}
    	    	}
			}
    	    
			conn.commit();
			conn.close();
			rep.shutDown();
			
			long after = System.currentTimeMillis() - time;
			log_info_st4.append("Time spent in fusion: "+t.count_time(after)+" \n");
			log_info_st4.append("Results: \n");
			log_info_st4.append("    Resources substituted: "+cont_substitutions+"\n");
			log_info_st4.append("    Instances interlinked (sameAs): "+agreg_perf_match+"\n");
			log_info_st4.append("    Classes interlinked (equivalentClass): "+agreg_perf_match_class+"\n");
			log_info_st4.append("    Resources interlinked as Related: "+agreg_relations+"\n");
			log_info_st4.append("    Resources interlinked as Related because of structural conflict: "+agreg_relations_diff_est+"\n");
			log_info_st4.append("    Resources Not related: "+not_related+"\n");
			
			log_info_st4.append("Saving modifications after fusion ...\n");
			save_result_fusion(cont_substitutions, agreg_perf_match, agreg_relations, agreg_perf_match_class, agreg_relations_diff_est, not_related, t.count_time(after));
		}
		catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (MalformedQueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (UpdateExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Save the dump - ok
		String fn=export_database();
		log_info_st4.append("Source dataset dumped and found in folder step_4/dumps_fusion/"+fn+" \n");
		
		log_info_st4.append("Fusion process finished.");
		fuse.setEnabled(false);
		notice_fusion.setText("You have already done the fusion.");
		notice_fusion.setForeground(Color.blue);
		
		submis.setEnabled(true);
	}
	
	// Begin functions of fusion
	
	private void load_source_ds_to_db(){
		Repository rep;
		RepositoryConnection conn;
		File file = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_4"+FileSystems.getDefault().getSeparator().toString()+"db_auxiliar_fusion");
		// Enable the construction of a new database to receive the possible new datasets and their respective scores 
		if(file.exists()){
			delete(file);
		}
		rep = new SailRepository(new NativeStore(file));
		try {
			// create an empty model
			Model model = RDFDataMgr.loadModel(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"data_publishing_dataset.rdf");
			
			rep.initialize();
			ValueFactory f  = rep.getValueFactory();
			conn = rep.getConnection();
			conn.begin();
			
			StmtIterator iter = model.listStatements();
			while (iter.hasNext()) {
			    Statement stmt     = iter.nextStatement();  // get next statement
			    Resource  s   = stmt.getSubject();     // get the subject
			    Property  p = stmt.getPredicate();   // get the predicate
			    RDFNode o = stmt.getObject();
			    
			    URI subject = f.createURI(s.getURI());
				URI predicate = f.createURI(p.getURI());
				if(stmt.getObject().isLiteral()){
			    	Literal obj_2=null;
			    	obj_2 = f.createLiteral(o.asLiteral().getLexicalForm(), o.asLiteral().getDatatypeURI());
			    	conn.add(subject, predicate, obj_2);
			    }
			    else{
			    	URI obj_2 = f.createURI(stmt.getObject().asResource().getURI());
			    	conn.add(subject, predicate, obj_2);
			    }
			}
			conn.commit();
			conn.close();
			rep.shutDown();
		}
		catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String export_database(){
		Repository rep;
		RepositoryConnection conn;
		
		File dir_dumps_fusion = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_4"+FileSystems.getDefault().getSeparator()+"dumps_fusion");
    	if(!dir_dumps_fusion.exists())
    		dir_dumps_fusion.mkdir();
		File file = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_4"+FileSystems.getDefault().getSeparator().toString()+"db_auxiliar_fusion");
		rep = new SailRepository(new NativeStore(file));
		try{
			rep.initialize();
			conn = rep.getConnection();
			conn.begin();
			RepositoryResult<org.openrdf.model.Statement> statements =  conn.getStatements(null, null, null, true);
			org.openrdf.model.Model model = Iterations.addAll(statements, new LinkedHashModel());
			try {
				OutputStream out = new FileOutputStream(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_4"+FileSystems.getDefault().getSeparator()+"dumps_fusion"+FileSystems.getDefault().getSeparator()+"db_fusion_submission_"+Build_view_step4.id_submission+"_"+(submis.getSelectedItem().toString().replace("/", "-").replace(":", "-").replace(" - ", "_"))+".rdf");
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
			conn.close();
			rep.shutDown();
		}
		catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String fname="db_fusion_submission_"+Build_view_step4.id_submission+"_"+(submis.getSelectedItem().toString().replace("/", "-").replace(":", "-").replace(" - ", "_"))+".rdf";
		return fname;
	}
	
	private void load_data_fusion(){
		Integer cont_substitutions=0, agreg_perf_match=0, agreg_perf_match_class=0, agreg_relations=0, agreg_relations_diff_est=0, not_related=0; 
		String spent_time="", fusion_date="";
		
		String fname="";
		File f = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_4"+FileSystems.getDefault().getSeparator()+"dumps_fusion");
		if(f.exists()){
			File[] fl = f.listFiles();
	        for (int i=0; i<fl.length;i++){
	        	if(fl[i].getName().equalsIgnoreCase("result_fusion_submission_"+Build_view_step4.id_submission+"_"+(submis.getSelectedItem().toString().replace("/", "-").replace(":", "-").replace(" - ", "_"))+".xml")){
	        		fname=fl[i].getAbsolutePath();
	        	}
	        }
			
	        if(!fname.equalsIgnoreCase("")){
				File fXmlFile = new File(fname);
				DocumentBuilderFactory datasets = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder;
				try {
					dBuilder = datasets.newDocumentBuilder();
					Document doc = dBuilder.parse(fXmlFile);
					doc.getDocumentElement().normalize();
					NodeList nList = doc.getElementsByTagName("submission");
					
					for (int temp = 0; temp < nList.getLength(); temp++) {
						Node nNode = nList.item(temp);
								
						if (nNode.getNodeType() == Node.ELEMENT_NODE) {
							Element eElement = (Element) nNode;
							
							cont_substitutions = Integer.parseInt(eElement.getElementsByTagName("substituted_resources").item(0).getTextContent()); 
							agreg_perf_match = Integer.parseInt(eElement.getElementsByTagName("agregated_as_equal").item(0).getTextContent()); 
							agreg_perf_match_class = Integer.parseInt(eElement.getElementsByTagName("classes_agregated_as_equal").item(0).getTextContent()); 
							agreg_relations = Integer.parseInt(eElement.getElementsByTagName("agregated_as_related").item(0).getTextContent()); 
							agreg_relations_diff_est = Integer.parseInt(eElement.getElementsByTagName("agregated_as_related_structural_conflict").item(0).getTextContent()); 
							not_related = Integer.parseInt(eElement.getElementsByTagName("not_related").item(0).getTextContent()); 
							spent_time = eElement.getElementsByTagName("spent_time").item(0).getTextContent(); 
							fusion_date = eElement.getElementsByTagName("creation_moment").item(0).getTextContent();
						}
					}
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
				
				log_info_st4.setText("Fusion made in: "+fusion_date+" \n");
				log_info_st4.append("Time spent in fusion: "+spent_time+" \n");
				log_info_st4.append("Results: \n");
				log_info_st4.append("    Resources substituted: "+cont_substitutions+"\n");
				log_info_st4.append("    Instances interlinked (sameAs): "+agreg_perf_match+"\n");
				log_info_st4.append("    Classes interlinked (equivalentClass): "+agreg_perf_match_class+"\n");
				log_info_st4.append("    Resources interlinked as Related: "+agreg_relations+"\n");
				log_info_st4.append("    Resources interlinked as Related because of structural conflict: "+agreg_relations_diff_est+"\n");
				log_info_st4.append("    Resources Not related: "+not_related+"\n");
				log_info_st4.append("    Dump file can be found in: step_4/dumps_fusion/"+("db_fusion_submission_"+Build_view_step4.id_submission+"_"+(submis.getSelectedItem().toString().replace("/", "-").replace(":", "-").replace(" - ", "_"))+".rdf")+"\n");
				
				notice_fusion.setText("You have already done the fusion.");
				notice_fusion.setForeground(Color.blue);
				fuse.setEnabled(false);
				
	        }
		}
	}
	
	private void save_result_fusion(Integer cont_substitutions, Integer agreg_perf_match, Integer agreg_relations, Integer agreg_perf_match_class, Integer agreg_relations_diff_est, Integer not_related, String time){
		File dir_dumps_fusion = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_4"+FileSystems.getDefault().getSeparator()+"dumps_fusion");
    	if(!dir_dumps_fusion.exists())
    		dir_dumps_fusion.mkdir();
    	
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder ;
		
		try {
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("fusion");
			doc.appendChild(rootElement);
			
			Element seed = doc.createElement("submission");
			
			// set attribute to staff element
			Attr attr = doc.createAttribute("id");
			attr.setValue(Build_view_step4.id_submission+"");
			seed.setAttributeNode(attr);
			
			Element date_ = doc.createElement("date_hour");
			date_.appendChild(doc.createTextNode(submis.getSelectedItem().toString()));
			seed.appendChild(date_);
			
			Element subst_ = doc.createElement("substituted_resources");
			subst_.appendChild(doc.createTextNode(cont_substitutions+""));
			seed.appendChild(subst_);
			
			Element equal_ = doc.createElement("agregated_as_equal");
			equal_.appendChild(doc.createTextNode(agreg_perf_match+""));
			seed.appendChild(equal_);
			
			Element equal_class = doc.createElement("classes_agregated_as_equal");
			equal_class.appendChild(doc.createTextNode(agreg_perf_match_class+""));
			seed.appendChild(equal_class);
			
			Element related_ = doc.createElement("agregated_as_related");
			related_.appendChild(doc.createTextNode(agreg_relations+""));
			seed.appendChild(related_);
			
			Element related_diff_est = doc.createElement("agregated_as_related_structural_conflict");
			related_diff_est.appendChild(doc.createTextNode(agreg_relations_diff_est+""));
			seed.appendChild(related_diff_est);
			
			Element no_rel_ = doc.createElement("not_related");
			no_rel_.appendChild(doc.createTextNode(not_related+""));
			seed.appendChild(no_rel_);
			
			Element time_ = doc.createElement("spent_time");
			time_.appendChild(doc.createTextNode(time));
			seed.appendChild(time_);
			
			DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			Date date = new Date();
			Element creation_ = doc.createElement("creation_moment");
			creation_.appendChild(doc.createTextNode(df.format(date)));
			seed.appendChild(creation_);
			
			rootElement.appendChild(seed);
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			
			StreamResult result = new StreamResult(new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_4"+FileSystems.getDefault().getSeparator()+"dumps_fusion"+FileSystems.getDefault().getSeparator().toString()+"result_fusion_submission_"+Build_view_step4.id_submission+"_"+(submis.getSelectedItem().toString().replace("/", "-").replace(":", "-").replace(" - ", "_"))+".xml"));
			transformer.transform(source, result);
			   
		} 
		catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	// End functions of fusion
	
	private void delete(File element) {
	    if (element.isDirectory()) {
	        for(File f: element.listFiles()) {
	            this.delete(f);
	        }
	    }
	    element.delete();
	}
	
	public Vector<String> populate_combo_submissions(String mode){
		Vector<String> items=new Vector<String>();
		items.add("Choose submission");
		
		Build_view_step3 b3=new Build_view_step3();
		if(!Build_view_step3.id_supervisor.equalsIgnoreCase("0") && !Build_view_step3.id_project.equalsIgnoreCase("0")){
			String urlParameters="func=8&id_supervisor="+Build_view_step3.id_supervisor+"&id_project="+Build_view_step3.id_project;
			String result=b3.post(Build_view_step3.url_api,urlParameters).replace("ï»¿","");
			JsonNode submissions= b3.jsonToNode(result);
			if(submissions!=null){
				for(JsonNode sub:submissions){
					items.add(sub.get("date").asText());
				}
				save_last_submissions(submissions);
			}
		}
		else{
			if(!mode.equalsIgnoreCase("silent")){
				JOptionPane.showMessageDialog(null, "Log in first on the previous tab.", "Erro", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		return items;
	}
	
	class ItemChangeListener implements ItemListener{
	    public void itemStateChanged(ItemEvent event) {
	        if (event.getStateChange() == ItemEvent.SELECTED) {
	            Object item = event.getItem();
	            String filename= Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_4"+FileSystems.getDefault().getSeparator()+("count_targets_approved.txt");
		  		File h = new File(filename);
		  		if(h.exists()){
		  			delete(h);
		  		}
		  		try {
					h.createNewFile();
				} 
	  			catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		  		
		  		fuse.setEnabled(false);
    	    	log_info_st4.setText("");
	            if(!item.toString().equalsIgnoreCase("Choose submission")){
	            	Integer id=load_data_submission(item.toString());
	        	    Build_view_step4.id_submission=id;
	        	    
	        		String previous="";
	            	int individual_pms=0; // Count how many different resources obtained "perfect match" as result and decides how to execute the fusion
	            	Build_view_step3 b3=new Build_view_step3();
	        	    String urlParameters = "func=18&id_submission="+id;
	        	    JsonNode results = b3.jsonToNode(b3.post(Build_view_step3.url_api,urlParameters));
	        	    int cont_pm=0, cont_rel=0, cont_no_rel=0, total=0;
	        	    if(results==null){
	        	    	JOptionPane.showMessageDialog(null, "There are no results for this submissions.", "Erro", JOptionPane.ERROR_MESSAGE);
	        	    }
	        	    else{
		        	    for (JsonNode r:results){
		        	    	String current_uri = r.get("uri_source").asText();
		        	    	
		        	    	if(!previous.equalsIgnoreCase(current_uri)){
		        	    		
		        	    		if(!previous.equalsIgnoreCase("")){
		        	    			try{
								  		PrintWriter pw = new PrintWriter(new FileWriter(filename,true)); //the true will append the new data
									    pw.print(previous+"%$@"+individual_pms+System.getProperty("line.separator"));//appends the string to the file
									    pw.close();
									}
									catch(IOException ioe){
									    //System.err.println("IOException: " + ioe.getMessage());
									}
		        	    		}
		        	    		
		        	    		previous=current_uri;
		        	    		individual_pms=0;
		        	    	}
		        	    	
		        	    	int pm = r.get("cont_pm").asInt();
		        	    	int rel = r.get("cont_r").asInt();
		        	    	int norel = r.get("cont_nr").asInt();
		        	    	
		        	    	String source_type = r.get("type_source").asText();
		        	    	String target_type = r.get("type_target").asText();
		        	    	
		        	    	if((pm>rel) && (pm>norel)){
		        	    		if(source_type.equalsIgnoreCase(target_type)){
		        	    			cont_pm++;
		        	    			individual_pms++;
		        	    		}
		        	    		else{
		        	    			cont_rel++;
				        	    }
		        	    	}
		        	    	if(((rel>pm) && (rel>norel)) || ((rel==pm) && ((rel+pm)>norel))){
		        	    		cont_rel++;
		        	    	}
		        	    	if((norel>pm) && (norel>rel)){
		        	    		cont_no_rel++;
		        	    	}
		        	    	
		        	    	perf_match.setText(cont_pm+"");
		        	    	relate.setText(cont_rel+"");
		        	    	not_relate.setText(cont_no_rel+"");
		        	    	
		        	    	total++;
		        	    	
		        	    	if(total==Integer.parseInt(num_pairs_.getText())){
		        	    		notice_fusion.setText("All tasks were completed.");
		        	    		notice_fusion.setForeground(Color.green);
		        	    		
		        	    		fuse.setEnabled(true);
		        	    		load_data_fusion();
		        	    	}
		        	    	else{
		        	    		notice_fusion.setText("There are incomplete tasks.");
		        	    		notice_fusion.setForeground(Color.red);
		        	    	}
		        	    }
		        	    
		        	    try{
					  		PrintWriter pw = new PrintWriter(new FileWriter(filename,true)); //the true will append the new data
						    pw.print(previous+"%$@"+individual_pms+System.getProperty("line.separator"));//appends the string to the file
						    pw.close();
						}
						catch(IOException ioe){
						    //System.err.println("IOException: " + ioe.getMessage());
						}
	        	    }
	            }
	            else{
	        	    num_pairs_.setText("");
	        	    num_groups_.setText("");
	        	
	        		perf_match.setText("0");
        	    	relate.setText("0");
        	    	not_relate.setText("0");
        	    	
        	    	notice_fusion.setText("");
        	    	
        	    }
	        }
	    }       
	}
	
	public Integer load_data_submission(String in_date){
		int id=0;
		File fXmlFile = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_4"+FileSystems.getDefault().getSeparator().toString()+"crowd_project_submissions.xml");
		DocumentBuilderFactory datasets = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = datasets.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("submission");
			
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
						
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					id=Integer.parseInt(eElement.getAttribute("id"));
					
					String date = eElement.getElementsByTagName("date").item(0).getTextContent();
					if(date.equalsIgnoreCase(in_date)){
						Integer total_pairs = Integer.parseInt(eElement.getElementsByTagName("number_of_pairs").item(0).getTextContent()); 
						num_pairs_.setText(total_pairs+"");
						Integer number_groups = Integer.parseInt(eElement.getElementsByTagName("number_of_groups").item(0).getTextContent()); 
						num_groups_.setText(number_groups+"");
					}
				}
			}
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
		
		return id;
	}
	
	public void save_last_submissions(JsonNode submissions){
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder ;
		
		try {
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("navigation");
			doc.appendChild(rootElement);
			for(JsonNode sub:submissions){
				Element seed = doc.createElement("submission");
				
				// set attribute to staff element
				Attr attr = doc.createAttribute("id");
				attr.setValue(sub.get("id").asText());
				seed.setAttributeNode(attr);
				
				Element name_ = doc.createElement("number_of_pairs");
				name_.appendChild(doc.createTextNode(sub.get("n_pairs").asText()));
				seed.appendChild(name_);
				
				Element url_ = doc.createElement("number_of_groups");
				url_.appendChild(doc.createTextNode(sub.get("n_groups").asText()));
				seed.appendChild(url_);
				
				Element description_ = doc.createElement("date");
				description_.appendChild(doc.createTextNode(sub.get("date").asText()));
				seed.appendChild(description_);
				
				rootElement.appendChild(seed);
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			File dir_step4 = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_4");
        	if(!dir_step4.exists())
        		dir_step4.mkdir();
        	File fg = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_4"+FileSystems.getDefault().getSeparator().toString()+"config_crowdsourcing.xml");
        	if(!fg.exists()){
				fg.createNewFile();
			}
			StreamResult result = new StreamResult(new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_4"+FileSystems.getDefault().getSeparator().toString()+"crowd_project_submissions.xml"));
			transformer.transform(source, result);
			   
		} 
		catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
}
