package integrator.view;

import integrator.auto_mapping.Consult_mapping;
import integrator.auto_mapping.KmeansText;
import integrator.auto_mapping.Mapping_clustering;
import integrator.config.Input_publishing_dataset;
import integrator.dscrawler.LinkController;
import integrator.dscrawler.TextPreProcessingRanking;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.nativerdf.NativeStore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orsoncharts.util.json.JSONArray;

public class Build_view_step2 {
	private JTable table_ds;
	private DefaultTableModel model_ds = new DefaultTableModel();;
	
	private JTable table_url;
	private DefaultTableModel model_url;
	
	public static JTextField limit_sparql;
	public static JTextField cutoff_value;
	public static JButton start_mapping;
	
	private JTextArea log_info_sec2 = new JTextArea();
	private JProgressBar progressBar;
	
	public static ObjectMapper mapper = new ObjectMapper();
	
	public JPanel join_all_sections(){
		JPanel merge_panels = new JPanel(new BorderLayout());
		
		JPanel section_mapping = build_section_choose_ds();
		merge_panels.add("West",section_mapping);
		
		JPanel info_section = new JPanel(new BorderLayout());
		log_info_sec2.setEditable(false);
		JScrollPane p_log = new JScrollPane(log_info_sec2);
		p_log.setPreferredSize(new Dimension(600,200));
		p_log.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		info_section.add("North",p_log);
		
		progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        info_section.add("South",progressBar);
        
        merge_panels.add("East",info_section);
        
		return merge_panels;
	}
	
	public JPanel build_section_choose_ds(){
		JPanel section_choose_ds = new JPanel(new BorderLayout());
		
		JPanel delete_url = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton remove_urls = new JButton ("Remove URL");
		remove_urls.addActionListener(
			    new ActionListener(){
		            public void actionPerformed(ActionEvent e){
		            	int[] l = table_url.getSelectedRows();
						if(l.length==0){
							JOptionPane.showMessageDialog(null, "There are no selected URLs to remove of the list.", "Erro", JOptionPane.ERROR_MESSAGE);
						}
						else{
							for (int i=0; i<l.length;i++){
								model_url.removeRow(l[i]);
							}
						}
		            }
		        }   
		    );
		delete_url.add(remove_urls);
		
		String[] columnNames = new String[] {"Datasets"};
        String[][] rowData = new String[][] {{""}};
        model_ds = new DefaultTableModel(rowData, columnNames) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JPanel choose_urls = new JPanel(new FlowLayout(FlowLayout.CENTER));
        table_ds = new JTable();
		table_ds.setModel(model_ds);
		table_ds.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		//model_ds.removeRow(0);
		table_ds.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
        JScrollPane p_table = new JScrollPane(table_ds);
        p_table.setPreferredSize(new Dimension(300, 400));
        p_table.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		p_table.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
        JPanel action_ds = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton clear_selection_ds = new JButton("Clear selection");
        clear_selection_ds.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	table_ds.clearSelection();
	            }
	        }
        );
        action_ds.add(clear_selection_ds);
        
        JPanel jp_table_ds = new JPanel(new BorderLayout());
		jp_table_ds.add("North", p_table);
		jp_table_ds.add("South", action_ds);
		
        JButton get_urls = new JButton("Get URLs");
		get_urls.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	int[] indice = table_ds.getSelectedRows();
	            	if(indice.length==0){
						JOptionPane.showMessageDialog(null, "There are no selected datasets.", "Erro", JOptionPane.ERROR_MESSAGE);
					}
					else{
						File file = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_1"+FileSystems.getDefault().getSeparator().toString()+"rdf_database");
		        		Repository rep = new SailRepository(new NativeStore(file));
		        		
		        		/*int n_rows=model_url.getRowCount();
		    			for(int i=0;i<n_rows;i++){
		    				model_url.removeRow(i);
		    			}*/
		    			
		        		try {
		        			rep.initialize();
		        			RepositoryConnection conn = rep.getConnection();
		        			
		        			ArrayList<String> ds = new ArrayList<String>();
		        			for (int i=0; i<indice.length;i++){
		        				ds.add(model_ds.getValueAt(indice[i], 0).toString());
		        			}
		        			get_urls_from_datasets(ds, conn);
		        			conn.close();
		        			rep.shutDown();
		        		}
		        		catch (RepositoryException e1){
		        			
		        		}
					}
	            }
	        }   
	    );
        
		table_url = new JTable();
        //table_url.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
		String[] columnNames_urls = new String[] {"URLs"};
        String[][] rowData_urls = new String[][] {{""}};
		model_url = new DefaultTableModel(rowData_urls, columnNames_urls) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
		table_url.setModel(model_url);
		model_url.removeRow(0);
		table_url.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
        JScrollPane p_table_url = new JScrollPane(table_url, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        p_table_url.setPreferredSize(new Dimension(250, 400));
        p_table_url.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		p_table_url.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
        JPanel action_url = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton clear_selection_url = new JButton("Clear selection");
        clear_selection_url.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	table_url.clearSelection();
	            }
	        }
        );
        action_url.add(clear_selection_url);
        
        JPanel jp_table_url = new JPanel(new BorderLayout());
		jp_table_url.add("North", p_table_url);
		jp_table_url.add("South", action_url);
		
        choose_urls.add(jp_table_ds);
        choose_urls.add(get_urls);
        choose_urls.add(jp_table_url);
        
        JPanel heading_mapping = new JPanel (new BorderLayout());
        heading_mapping.add(delete_url);
        heading_mapping.add(choose_urls);
        
        JPanel actions_mapping = new JPanel(new GridLayout(3,1));
        
        JPanel limit = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel limit_ = new JLabel("How much pairs to align?");
        limit_sparql = new JTextField(10);
        limit.add(limit_); limit.add(limit_sparql);
        
        JPanel cutoff = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel cutoff_ = new JLabel("Minimum confidence value (0 to 1):");
        cutoff_value = new JTextField(10);
        cutoff.add(cutoff_); cutoff.add(cutoff_value);
        
        JPanel start = new JPanel(new FlowLayout(FlowLayout.LEFT));
        start_mapping = new JButton("Start mapping");
		start_mapping.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	Thread_for_Mapping t1=new Thread_for_Mapping();  
            		t1.start(); 
	            }
	        }   
	    );
		start.add(start_mapping);
		
		actions_mapping.add(limit);
		actions_mapping.add(cutoff);
        actions_mapping.add(start);
		
        section_choose_ds.add("North", choose_urls);
        section_choose_ds.add("South", actions_mapping);
        
        JPanel section_complete = new JPanel(new BorderLayout());
        section_complete.add("North", delete_url);
        section_complete.add("South", section_choose_ds);
        
		return section_complete;
	}
	
	 class Thread_for_Mapping extends Thread{  
    	public void run(){  
    		send_for_mapping();  
    	}  
    }
	 
	public void send_for_mapping(){
		Integer limit=null;
		Double cut_off=0.4;
		
		if(!limit_sparql.getText().equalsIgnoreCase("")){
			limit = Integer.parseInt(limit_sparql.getText());
		}
		
		if(!cutoff_value.getText().equalsIgnoreCase("")){
			cut_off = Double.valueOf(cutoff_value.getText());
		}

		int[] indice = table_url.getSelectedRows();
    	if(indice.length==0){
			JOptionPane.showMessageDialog(null, "There are no selected URLs.", "Erro", JOptionPane.ERROR_MESSAGE);
		}
		else{
			String path=Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish";
			ArrayList<String> urls = new ArrayList<String>();
			ArrayList<String> types = new ArrayList<String>();
			
			Map<String,Integer> pairs_by_ds = new HashMap<String,Integer>();
			
			for (int i=0; i<indice.length;i++){
				
				String url=model_url.getValueAt(indice[i], 0).toString();
				if(!urls.contains(url)){
					urls.add(url);
					String type = (url.split("] ")[1].indexOf("sparql")!=-1) ? "endpoint" : "file";
					types.add(type);
				}
			}
    		
			int total=urls.size();
			
			//Consult_mapping c = new Consult_mapping();
			Mapping_clustering c = new Mapping_clustering();
			log_info_sec2.append("Pairs mapping started.\n");
			limit_sparql.setEnabled(false);
			cutoff_value.setEnabled(false);
			start_mapping.setEnabled(false);
			
			progressBar.setValue(0);
			
			String filename= path+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"charts";
			File dir_chart = new File(filename);
			if(dir_chart.exists()){
				delete(dir_chart);
			}
			dir_chart.mkdir();
			
			File file = new File(path+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"result_metrics.csv");
			if(file.exists()){
				delete(file);
			}
			try {
				file.createNewFile();
				
				FileWriter fw = new FileWriter(file.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
			
				bw.write("URLOrigin,URI-Source,Source,URI-Target,Target,Levenshtein,Jaro-Winkler,Jaccard,Dice,Refined Index,Double Metaphone,Monge-Elkan");
				bw.newLine();
				bw.close();
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			File file_external = new File(path+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"result_external_sameas.csv");
			if(file_external.exists()){
				delete(file_external);
			}
			try {
				file_external.createNewFile();
				
				FileWriter fw = new FileWriter(file_external.getAbsoluteFile());
				BufferedWriter bw = new BufferedWriter(fw);
			
				bw.write("URI-Target,URI-External-SameAs");
				bw.newLine();
				bw.close();
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			File file_log_mapping = new File(path+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"log_mapping.txt");
			// Enable the construction of the file with datasets' score information
			if(file_log_mapping.exists()){
				delete(file_log_mapping);
			}
			try {
				file_log_mapping.createNewFile();
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			long time=System.currentTimeMillis();
			Input_publishing_dataset ir = new Input_publishing_dataset();
			ArrayList<ArrayList<String>> a = ir.get_input_labels(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1");
			
			String ds_anterior=""; Integer cont_urls=0, number_of_pairs=0;
			for(int i=0; i<urls.size(); i++){
				String ds=urls.get(i).toString().split("] ")[0].replace("[", "").replace("]", "");
				String url=urls.get(i).toString().split("] ")[1].replace("\"","");
				
				// Cria o arquivo que vai armazenar os dados das métricas dos pares de cada URL para formar o dataset do gráfico depois
				File file_data_metric = new File(path+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"temp_data_metric.csv");
				if(file_data_metric.exists()){
					delete(file_data_metric);
				}
				try {
					file_data_metric.createNewFile();
					
					FileWriter fw = new FileWriter(file_data_metric.getAbsoluteFile());
					BufferedWriter bw = new BufferedWriter(fw);
				
					bw.write("value,type of metric,pair id");
					bw.newLine();
					bw.close();
				} 
				catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				Integer num_pairs_url=0;
				
				// Variáveis que mudam com o dataset
				
				if(!ds.equalsIgnoreCase(ds_anterior)){
					cont_urls++;
					ds_anterior=ds;
					number_of_pairs=0; // Cumulativo para cada url alvo também, só zera quando mudar o dataset, isso é utilizado para manter o índice no array json
				}
				
				LinkController l = new LinkController();
				
				if(!url.contains("bio2rdf.org")){
					url= (l.get_redirected_url(url)!=null) ? l.get_redirected_url(url) : url;
				}
				
				URL s;
				try {
					s = new URL(url);
					String type = (url.indexOf("sparql")!=-1) ? "endpoint" : "file" ;
					String url_test = (type.equalsIgnoreCase("endpoint")) ? url+"?query=SELECT++*%0AWHERE%0A++%7B+%3Furitarget++%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23label%3E++%3Flabeltarget+%7D%0A%20limit%2010" : url;
					
					TextPreProcessingRanking t = new TextPreProcessingRanking();
					int size = t.getFileSize(s);
					if(url.contains("bio2rdf.org")){
						url_test="http://virtuoso.openlifedata.org/sparql"+"?query=SELECT++*%0AWHERE%0A++%7B+%3Furitarget++%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23label%3E++%3Flabeltarget+%7D%0A%20limit%2010";
						size=1;
					}
					int code_test = t.test_code(url_test);
					
					if(code_test==200 && (size!=-1 || (size>0 && size<=10000000)) && !url.startsWith("http://www.w3.org/")){
						if(!url.startsWith("http://www.w3.org/")){
							log_info_sec2.append("Querying URL "+url+"\n");
							
							ArrayList<String> labels = a.get(0);
							ArrayList<String> uris = a.get(1);
							Iterator<String> ur = uris.iterator();
							for (String concept:labels){
								String uri = ur.next();
								
								String qu=c.mount_query(path, url, types.get(i), limit, cut_off, concept, uri);
								ArrayList<Object> res_temp = c.extract_remote_data(path, qu, url, concept, uri, types.get(i), limit, cont_urls, num_pairs_url); //Use when Mapping_clustering 
								int n_pairs_by_ds= (Integer) res_temp.get(1);
								number_of_pairs+=n_pairs_by_ds;
								num_pairs_url=(Integer) res_temp.get(0);
								
								//int n_pairs_by_ds=c.extract_remote_data(path, qu, url, concept, uri); //Use when Consult_mapping
								
								if(pairs_by_ds.containsKey(ds)){
									int value=pairs_by_ds.get(ds).intValue();
									pairs_by_ds.put(ds, value+n_pairs_by_ds);
								}
								else{
									pairs_by_ds.put(ds, n_pairs_by_ds);
								}
							}
							progressBar.setValue(((1+i)*100)/total);
							
						}
					}
				} 
				catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// Desenho de gráfico para visualizar o comportamento das métricas em cada URL alvo
				draw_metric_chart(num_pairs_url,path,i);
					
				System.out.println("Finish 1");
			}
			
			long now=System.currentTimeMillis()-time;
			TextPreProcessingRanking t = new TextPreProcessingRanking();
			progressBar.setValue(100);
			log_info_sec2.append("Pairs mapping finished.\n");
			log_info_sec2.append("Time spent by the pairs mapping: "+t.count_time(now)+"\n");
			log_info_sec2.append("Results by Dataset: \n");
			Iterator it = pairs_by_ds.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
		        log_info_sec2.append(pair.getKey().toString().replace("'", "") + ": " + pair.getValue()+" pairs\n");
		        it.remove(); // avoids a ConcurrentModificationException
		    }
		    
			limit_sparql.setEnabled(true);
			cutoff_value.setEnabled(true);
			start_mapping.setEnabled(true);
		}	
	}
	
	public void draw_metric_chart(Integer n_pairs, String path, Integer index_url_atual){
		String csvFile = Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"temp_data_metric.csv";
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		
		int cont=0;
		DefaultCategoryDataset ds = new DefaultCategoryDataset();
		try {
			br = new BufferedReader(new FileReader(csvFile));
			while ((line = br.readLine()) != null) {
				if(cont!=0){
					String[] features = line.split(cvsSplitBy);
					ds.addValue(Double.parseDouble(features[0]), features[1],features[2]);
				}
				
				cont++;
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		catch (IOException e) {
			e.printStackTrace();
		} 
		finally {
			if (br != null) {
				try {
					br.close();
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		if(n_pairs>1){
			String filename= path+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"charts";
			File dir_chart = new File(filename);
			if(!dir_chart.exists()){
				dir_chart.mkdir();
			}
			
			JFreeChart chart = ChartFactory.createLineChart(
		            "Evolution of metrics", "Pairs", "Values",
		            ds, PlotOrientation.VERTICAL, true, true, false);
			try {
				ChartUtilities.saveChartAsPNG(new File(filename+FileSystems.getDefault().getSeparator()+"n_pairs-"+n_pairs+"_source-url-"+index_url_atual+".png"), chart, 600, 300);
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
	
	public void get_datasets(String path){
		
		File file = new File(path+FileSystems.getDefault().getSeparator()+"rdf_database");
		Repository rep = new SailRepository(new NativeStore(file));
		try {
			rep.initialize();
			RepositoryConnection conn = rep.getConnection();
			
			conn.begin();
			String queryString = "prefix dscrawler: <http://localhost/DSCrawler/> "
					+ "select distinct ?name  "
					+ "where {"
					+ " ?uri dscrawler:hasName ?name . "
					+ " ?uri dscrawler:hasScore ?score . "
					+ "} "
					+ "order by desc(?score) ";
			TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

			TupleQueryResult result = tupleQuery.evaluate();
			model_ds.setRowCount(0);
			
			try {
	            while (result.hasNext()) {  // iterate over the result
					BindingSet bindingSet = result.next();
					Value name = bindingSet.getValue("name");
					model_ds.addRow(new Object[]{name.toString().replace("^^<http://www.w3.org/2001/XMLSchema#string>", "")});
	            }
			}
			finally {
				result.close();
			}
			
			conn.close();
			rep.shutDown();
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
	}
	
	public void get_urls_from_datasets(ArrayList<String> ds, RepositoryConnection conn){
		try {
			conn.begin();
			
			for (String d:ds){
				d=d.replace("\"", "'");
				String queryString = "prefix dscrawler: <http://localhost/DSCrawler/> "
						+ "select ?url "
						+ "where {"
						+ " ?uri dscrawler:hasName "+d+"^^<http://www.w3.org/2001/XMLSchema#string> . "
						+ " ?uri dscrawler:hasURL ?url . "
						+ "} ";
				System.out.println(d);
				TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
				TupleQueryResult result = tupleQuery.evaluate();
				
	            while (result.hasNext()) {  // iterate over the result
					BindingSet bindingSet = result.next();
					Value name = bindingSet.getValue("url");
					String new_url=name.toString().replace("^^<http://www.w3.org/2001/XMLSchema#string>", "");
					if(!existing_url(new_url)){
						model_url.addRow(new Object[]{"["+d+"] "+new_url});
					}
	            }
	            
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
	}
	
	public boolean existing_url(String new_url){
		boolean ex=false;
		
		int n_ur = model_url.getRowCount();
		for (int i=0; i<n_ur; i++){
			if(model_url.getValueAt(i, 0).toString().indexOf(new_url)!=-1){
				ex= true;
				break;
			}
		}
		return ex;
	}
	
}
