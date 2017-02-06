package integrator.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.XSD;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
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
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.nativerdf.NativeStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import integrator.config.Input_publishing_dataset;
import integrator.dscrawler.DSCrawler;
import integrator.dscrawler.LinkController;
import integrator.dscrawler.TextPreProcessingRanking; 

public class Build_view_step1 {
	private JLabel app_directory_;
	private JButton app_directory;
	public static String application_dir;
	
	private JTextField url;
	private JRadioButton sparql_option;
	private JRadioButton uri_option;
	private JRadioButton upload_option;
	private JButton path_file;
	private String file_uploaded; 
	
	private JPanel section_seeds;
	private JTextField url_seed;
	private JCheckBox pagination;
	private JButton treat_url_seed;
	public JTextArea log_info = new JTextArea();
	private JTable table;
	private DefaultTableModel model;
	private JButton save_seeds;
	private JButton remove_seeds;
	
	private JTextField top_k;
	private JTextField limit;
	private JButton start_crawling;
	private JButton start_analysis;
	private JTable table_results;
	private DefaultTableModel model_results;
	public static JProgressBar progressBar = new JProgressBar();
	private JButton see_score_graph;
	
    class Thread_for_crawling extends Thread{  
    	public void run(){  
    		do_crawling();  
    	}  
    }
    
    class Thread_for_ranking extends Thread{  
    	public void run(){  
    		do_ranking();  
    	}  
    }
    
    public void do_crawling(){
    	unable_fields_section_seeds();
    	start_crawling.setEnabled(false);
    	start_analysis.setEnabled(false);
    	top_k.setEnabled(false);
    	limit.setEnabled(false);
    	
    	log_info.append("Crawler task started.\n");
    	
    	TextPreProcessingRanking t = new TextPreProcessingRanking();
		long time = System.currentTimeMillis();
    	final DSCrawler d = new DSCrawler();
    	d.setPathToNavigationFile(application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1");
    	d.active_search_dataset(true);
    	d.start_crawling(null, null, "");
    	long after = System.currentTimeMillis() - time;
    	log_info.append("Total during of crawling: "+t.count_time(after)+" \n");
    	log_info.append("Crawler task finished.\n");
    	progressBar.setValue(30);
    	
    	enable_fields_section_seeds();
    	start_crawling.setEnabled(true);
    	start_analysis.setEnabled(true);
    	top_k.setEnabled(true);
		limit.setEnabled(true);
	}
    
    public void do_ranking(){
    	unable_fields_section_seeds();
    	start_crawling.setEnabled(false);
    	start_analysis.setEnabled(false);
    	top_k.setEnabled(false);
    	limit.setEnabled(false);
    	
    	TextPreProcessingRanking t = new TextPreProcessingRanking();
		long time = System.currentTimeMillis();
		Integer lim=null;
		if(!limit.getText().equalsIgnoreCase("")){
			lim = Integer.parseInt(limit.getText());
		}
		log_info.append("Relevance analysis started.\n");
		t.analysis_for_ranking(application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1", lim);
		long after = System.currentTimeMillis() - time;
		t.close_database();
		log_info.append("Total during of relevance analysis: "+t.count_time(after)+" \n");
    	log_info.append("Relevance analysis finished.\n");
    	
    	feed_results_table(application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1");
    	log_info.append("Datasets table reloaded.\n");
		progressBar.setValue(100);
		
		enable_fields_section_seeds();
    	start_crawling.setEnabled(true);
    	start_analysis.setEnabled(true);
    	top_k.setEnabled(true);
		limit.setEnabled(true);
		
		see_score_graph.setEnabled(true);
    }
	public JPanel join_all_sections(){
		JPanel merge_panels = new JPanel(new GridLayout(1,2));
		
		JPanel section_input = build_input_publishing_data();
		section_seeds = build_seeds_section();
		JPanel section_results = build_section_results();
		
		JPanel panel1 = new JPanel(new BorderLayout());
		panel1.add("North", section_input);
		panel1.add("South", section_seeds);
		
		merge_panels.add(panel1);
		merge_panels.add(section_results);
		
		return merge_panels;
	}
	
	public JPanel build_input_publishing_data(){
		TitledBorder config = new TitledBorder("Configuration");
		JPanel panel_input = new JPanel(new BorderLayout());
		panel_input.setBorder(config);

		final JPanel panel_data_input = new JPanel (new GridLayout(2,1));
				
		JPanel choice_directory_application = new JPanel(new FlowLayout(FlowLayout.CENTER));
		app_directory_ = new JLabel ("Choose the directory where this app will work:");
		app_directory = new JButton("Choose...");
		app_directory.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	                JFileChooser fc = new JFileChooser();
	                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	                int res = fc.showOpenDialog(null);
	                    
	                if(res == JFileChooser.APPROVE_OPTION){
                        application_dir = fc.getSelectedFile().getAbsolutePath();
                        String sep = (FileSystems.getDefault().getSeparator().toString().equalsIgnoreCase("\\")) ? "\\\\" : "/";
                        String[] partes = application_dir.split(sep);
                    	log_info.append("The application directory chosen was "+partes[partes.length-1]+"\n");
                    	
                    	app_directory.setEnabled(false);
                    	File f = new File(application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish");
			        	if(!f.exists()){
			        		panel_data_input.setVisible(true);
			        	}
			        	else{
			        		Window window = SwingUtilities.getWindowAncestor(section_seeds);

			        		//Cast it to JFrame
			        		JFrame frame = (JFrame) window;

			        		 //Now, change the title
			        		frame.setTitle("yPublish - Current workspace: "+partes[partes.length-1]+"/");
			        		// Pull informations from the files generated along the actions and turn visible according to the files
			        		enable_fields_section_seeds();
			        		recover_seeds_configuration();
			        		
			        		if(model.getRowCount()>0){
				        		top_k.setEnabled(true);
				        		start_crawling.setEnabled(true);
				        		start_analysis.setEnabled(true);
				        		limit.setEnabled(true);
			        		}
			        		
			        		feed_results_table(application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1");
			        		
			        		File a = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_3"+FileSystems.getDefault().getSeparator().toString()+"config_crowdsourcing.xml");
			        		if(a.exists()){
			        			Build_view_step3 obj_3 = new Build_view_step3();
			        			obj_3.read_crowd_config("step_1");
			        			Build_view_step3.model_combo = new DefaultComboBoxModel<String>(obj_3.populate_combo_submissions());
			        			Build_view_step3.subs.setModel(Build_view_step3.model_combo);
			        		}
			        		
			        	}
    	            }
                    else {
                    	JOptionPane.showMessageDialog(null, "You did not select any directory.", "Erro", JOptionPane.ERROR_MESSAGE); 
                	}
	            }
	        }   
	    );
		choice_directory_application.add(app_directory_);
		choice_directory_application.add(app_directory);
		ChangeListener ch = new ChangeListener(){
			public void stateChanged(ChangeEvent changEvent) {
		        if(sparql_option.isSelected() || uri_option.isSelected()){
		        	url.setVisible(true);
		        	path_file.setVisible(false);
		        }
		        if(upload_option.isSelected()){
		        	url.setVisible(false);
		        	path_file.setVisible(true);
		        }
			}
        };
        
		JPanel panel_options_input = new JPanel (new FlowLayout(FlowLayout.LEFT));
		sparql_option = new JRadioButton("Sparql Endpoint");
		sparql_option.setSelected(true);
		sparql_option.addChangeListener(ch);
		uri_option = new JRadioButton("Remote URI");
		uri_option.addChangeListener(ch);
		upload_option = new JRadioButton("Upload file");
		upload_option.addChangeListener(ch);
		
		ButtonGroup group = new ButtonGroup();
	    group.add(sparql_option);
	    group.add(uri_option);
	    group.add(upload_option);
		panel_options_input.add(sparql_option);
		panel_options_input.add(uri_option);
		panel_options_input.add(upload_option);
	    
		url = new JTextField("URI");
		url.setColumns(20);
		
		path_file = new JButton("Choose RDF file");
		path_file.setVisible(false);
		path_file.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	FileNameExtensionFilter filter = new FileNameExtensionFilter("rdf", "ttl", "nt", "n3","xml");
	            	JFileChooser fc = new JFileChooser();
	            	fc.setFileFilter(filter);
	                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
	                int res = fc.showOpenDialog(null);
	                    
	                if(res == JFileChooser.APPROVE_OPTION){
                        file_uploaded = fc.getSelectedFile().getAbsolutePath();
                        String sep = (FileSystems.getDefault().getSeparator().toString().equalsIgnoreCase("\\")) ? "\\\\" : "/";
                        String[] partes = file_uploaded.split(sep);
                    	log_info.setText(log_info.getText()+"\n The input file chosen was "+partes[partes.length-1]);
                    	
                    }
                    else {
                    	JOptionPane.showMessageDialog(null, "You did not select any file.", "Erro", JOptionPane.ERROR_MESSAGE); 
                	}
	            }
	        }   
	    );
		
		JButton treat_data_input = new JButton("Send");
		treat_data_input.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	Input_publishing_dataset i = new Input_publishing_dataset();
	        		String error="";
		        	if(application_dir.equalsIgnoreCase("")){
		        		error="choose the workspace directory";
		        	}
		        	if(sparql_option.isSelected() || uri_option.isSelected()){
	            		if(url.getText().equalsIgnoreCase("")){
			        		if(!error.equalsIgnoreCase("")){
			        			error+="; ";
			        		}
			        		if(sparql_option.isSelected())
			        			error+="set the URL of an endpoint";
			        		if(uri_option.isSelected())
			        			error+="set the URI of a remote rdf file";
			        	}
			        	else{
			        		if(sparql_option.isSelected())
			        			if(!i.verify_rdf_endpoint(url.getText())){
			        				if(!error.equalsIgnoreCase("")){
					        			error+="; ";
					        		}
			        				error+="set a valid URL for endpoint";
			        			}
			        		if(uri_option.isSelected())
			        			if(!i.verify_rdf_uri(url.getText())){
			        				if(!error.equalsIgnoreCase("")){
					        			error+="; ";
					        		}
			        				error+="set a valid URI of a remote file";
			        			}
			        	}
			        }
			        if(upload_option.isSelected()){
			        	if(file_uploaded.equalsIgnoreCase("")){
			        		if(!error.equalsIgnoreCase("")){
			        			error+="; ";
			        		}
			        		error+="You did not choose an rdf file";
			        	}
			        	else{
			        		if(i.verify_rdf_uri(file_uploaded)){
			        			if(!error.equalsIgnoreCase("")){
				        			error+="; ";
				        		}
		        				error+="choose a valid rdf file";
			        		}
			        	}
			        }
			        
			        if(error.equalsIgnoreCase("")){
			        	File f = new File(application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish");
			        	if(!f.exists()){
			        		File dir_step1 = new File(application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1");
			        		File dir_step2 = new File(application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_2");
				        	dir_step1.mkdirs();
				        	dir_step2.mkdirs();
				        	
				        	Build_view parent = new Build_view();
				        	String sep = (FileSystems.getDefault().getSeparator().toString().equalsIgnoreCase("\\")) ? "\\\\" : "/";
	                        String[] partes = application_dir.split(sep);
			        		parent.setTitle("yPublish - Current workspace: "+partes[partes.length-1]+"/");
			        		
			        		if(sparql_option.isSelected())
			        			i.extract_data_from_endpoint(url.getText(), application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"data_publishing_dataset.rdf");
			        		if(uri_option.isSelected() || upload_option.isSelected())
			        			i.extract_data_from_file_uri(url.getText(), application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"data_publishing_dataset.rdf");
			        		if(upload_option.isSelected())
			        			i.extract_data_from_file_uri(file_uploaded, application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"data_publishing_dataset.rdf");
			        		
			        		url.setText("");
			        		panel_data_input.setVisible(false);
			        		enable_fields_section_seeds();
			        		
			        		JOptionPane.showMessageDialog(null, "Publishing dataset loaded.", "Information", JOptionPane.PLAIN_MESSAGE);
			        		
			        	}
			        }
			        else{
			        	JOptionPane.showMessageDialog(null, "Error(s) found: "+error, "Erro", JOptionPane.ERROR_MESSAGE);
			        }
	            }
	        }   
	    );
		
		panel_data_input.add(panel_options_input);
		JPanel data_field = new JPanel(new FlowLayout(FlowLayout.LEFT));
		data_field.add(url);
		data_field.add(path_file);
		data_field.add(treat_data_input);
		
		panel_data_input.add(data_field);
		panel_data_input.setVisible(false);
		
		panel_input.add("North",choice_directory_application);
		panel_input.add("South",panel_data_input);
		return panel_input;
	}
	
	
	public JPanel build_seeds_section(){
		TitledBorder config = new TitledBorder("Seeds configuration");
		section_seeds = new JPanel(new BorderLayout());
		section_seeds.setBorder(config);
		
		// Insertion of seeds
		JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
		url_seed = new JTextField(20);
		url_seed.setEnabled(false);
		
		pagination = new JCheckBox("Verify pages");
		pagination.setEnabled(false);
		
		treat_url_seed = new JButton("Add seed");
		treat_url_seed.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	LinkController lc = new LinkController();
	            	if(lc.verify_url(url_seed.getText())){
	            		String pages="No";
	            		if(pagination.isSelected()){
	            			pages = "Yes";
	            		}
	            		String structure_="ckan";
	            		if(url_seed.getText().startsWith("http://download.bio2rdf.org") || url_seed.getText().startsWith("http://download.openbiocloud.org") || url_seed.getText().startsWith("http://bio2rdf.org") || url_seed.getText().startsWith("http://www.bio2rdf.org") || url_seed.getText().startsWith("http:bio2rdf.org") || url_seed.getText().startsWith("http:www.bio2rdf.org")){
	            			structure_="bio2rdf";
	            		}
	            		model.addRow(new Object[]{url_seed.getText(), structure_, pages});
	            		
	            		url_seed.setText("");
	            		pagination.setSelected(false);
	            	}
	            	else{
			        	JOptionPane.showMessageDialog(null, "Enter with a valid URL", "Erro", JOptionPane.ERROR_MESSAGE);	            		
	            	}
	            }
	        }   
	    );
		treat_url_seed.setEnabled(false);
		
		header.add(url_seed);
		header.add(pagination);
		header.add(treat_url_seed);
		
		// Actions (exclusion and saving) for the table of seeds
		JPanel actions_table_seeds = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		save_seeds = new JButton ("Save");
		save_seeds.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	if(table.getRowCount()==0){
	            		JOptionPane.showMessageDialog(null, "Insert at least one seed to save.", "Erro", JOptionPane.ERROR_MESSAGE);
	            	}
	            	else{
	            		top_k.setEnabled(true);
	            		start_crawling.setEnabled(true);
	            		start_analysis.setEnabled(true);
	            		limit.setEnabled(true);
	            		
	            		generate_config_seeds_file();
	            		JOptionPane.showMessageDialog(null, "File with seeds saved.", "Information", JOptionPane.PLAIN_MESSAGE);
	            	}
	            }
	        }   
	    );
		save_seeds.setEnabled(false);
		
		// Generate an XML of the seeds
		remove_seeds = new JButton ("Remove");
		remove_seeds.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	generate_config_seeds_file();
					int[] l = table.getSelectedRows();
					if(l.length==0){
						JOptionPane.showMessageDialog(null, "There are no selected seeds to remove.", "Erro", JOptionPane.ERROR_MESSAGE);
					}
					else{
						for (int i=0; i<l.length;i++){
							model.removeRow(l[i]);
						}
						generate_config_seeds_file();
	            		JOptionPane.showMessageDialog(null, "File with seeds updated.", "Information", JOptionPane.PLAIN_MESSAGE);
	            	
					}
					
					if(table.getRowCount()!=0){
	            		top_k.setEnabled(true);
	            		start_crawling.setEnabled(true);
	            		start_analysis.setEnabled(true);
	            		limit.setEnabled(true);
	            	}
					else{
						top_k.setEnabled(false);
						start_crawling.setEnabled(false);
	            		start_analysis.setEnabled(false);
	            		limit.setEnabled(false);
					}
	            }
	        }   
	    );
		remove_seeds.setEnabled(false);
		
		// The same and table.remove(index)
		actions_table_seeds.add(save_seeds);
		actions_table_seeds.add(remove_seeds);
		
		// constructs the table
        String[] columnNames = new String[] {"URL", "Structure", "Pagination"};
        String[][] rowData = new String[][] {{"","",""}};
        model = new DefaultTableModel(rowData, columnNames) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable();
		table.setModel(model);
		model.removeRow(0);
		table.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
        JScrollPane p_table = new JScrollPane(table);
        p_table.setPreferredSize(new Dimension(600, 200));
        p_table.getVerticalScrollBar();
        
		// Informations Panel
		log_info.setEditable(false);
		JScrollPane p_log = new JScrollPane(log_info);
		p_log.setPreferredSize(new Dimension(600,100));
		p_log.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		JPanel aux_1 = new JPanel (new GridLayout(2,1));
        aux_1.add(header);
        aux_1.add(actions_table_seeds);
        
        JPanel aux_2 = new JPanel (new BorderLayout(2,1));
        aux_2.add("North",p_table);
        aux_2.add("South",p_log);
        
		section_seeds.add("North",aux_1);
		section_seeds.add("South",aux_2);
		
		//section_seeds.setVisible(false);
		return section_seeds;
	}           
	
	public JPanel build_section_results(){
		TitledBorder config = new TitledBorder("Results for Crawling and Ranking");
		JPanel section_results = new JPanel (new BorderLayout());
		section_results.setBorder(config);
		
		JPanel jp_k = new JPanel (new FlowLayout(FlowLayout.LEFT));
		JLabel label_k = new JLabel ("Return k Datasets: ");
		top_k = new JTextField(10);
		top_k.setEnabled(false);
		jp_k.add(label_k);
		jp_k.add(top_k);
		
		JPanel jp_limit = new JPanel (new FlowLayout(FlowLayout.LEFT));
		JLabel label_limit = new JLabel("Limit tree verification: ");
		limit = new JTextField(10);
		limit.setEnabled(false);
		jp_limit.add(label_limit);
		jp_limit.add(limit);
		
		see_score_graph = new JButton("See score graph");
		see_score_graph.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	push_data_for_graphic(application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1");
	            }
	        }   
	    );
		see_score_graph.setEnabled(false);
		
		start_crawling = new JButton("Start Crawling");
		start_crawling.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	if(model.getRowCount()>0){
		            	//task.addPropertyChangeListener(new Build_view_step1());
		                //task.execute();
	            		Thread_for_crawling t1=new Thread_for_crawling();  
	            		t1.start(); 
	            	}
	            	else{
	            		JOptionPane.showMessageDialog(null, "Insert at least one seed to start the process.", "Erro", JOptionPane.ERROR_MESSAGE);
	            	}
	            }
	        }   
	    );
		start_crawling.setEnabled(false);
		
		start_analysis = new JButton("Start Ranking");
		start_analysis.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	File ds_info = new File(application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()+"dataset_information.xml");
	            	if(ds_info.exists()){
		            	//task.addPropertyChangeListener(new Build_view_step1());
		                //task.execute();
	            		Thread_for_ranking t1=new Thread_for_ranking();  
	            		t1.start(); 
	            	}
	            	else{
	            		JOptionPane.showMessageDialog(null, "You need to do the crawling part before ranking.", "Erro", JOptionPane.ERROR_MESSAGE);
	            	}
	            }
	        }   
	    );
		start_analysis.setEnabled(false);
		
		progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
		header.add(jp_k);
		header.add(jp_limit);
		
		JPanel action_header = new JPanel(new FlowLayout(FlowLayout.LEFT));
		action_header.add(start_crawling);
		action_header.add(start_analysis);
		action_header.add(see_score_graph);
		
		JPanel union = new JPanel(new BorderLayout());
		union.add("North", header);
		union.add("South", action_header);
		
		JPanel adjust = new JPanel(new BorderLayout());
		adjust.add("North", union);
		adjust.add("South", progressBar);
		
		String[] columnNames = new String[] {"Dataset", "Score"};
        String[][] rowData = new String[][] {{"",""}};
        model_results = new DefaultTableModel(rowData, columnNames) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table_results = new JTable();
		table_results.setModel(model_results);
		model_results.removeRow(0);
		table_results.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
        JScrollPane p_table = new JScrollPane(table_results);
        p_table.setPreferredSize(new Dimension(600, 200));
        p_table.getVerticalScrollBar();
        
		section_results.add("North", adjust);
		section_results.add("South", p_table);
        
		return section_results;
	}
	
	public void enable_fields_section_seeds(){
		url_seed.setEnabled(true);
		pagination.setEnabled(true);
		treat_url_seed.setEnabled(true);
		save_seeds.setEnabled(true);
		remove_seeds.setEnabled(true);
	}
	public void unable_fields_section_seeds(){
		url_seed.setEnabled(false);
		pagination.setEnabled(false);
		treat_url_seed.setEnabled(false);
		save_seeds.setEnabled(false);
		remove_seeds.setEnabled(false);
	}
	
	public void push_data_for_graphic(String path){
		String lim= (top_k.getText().equalsIgnoreCase("")) ? "20" : top_k.getText();
		int limit_=20;
		try{
			limit_=Integer.parseInt(lim);
		}
		catch(Exception e){
			
		}
		
		File file = new File(path+FileSystems.getDefault().getSeparator()+"rdf_database");
		Repository rep = new SailRepository(new NativeStore(file));
		try {
			rep.initialize();
			RepositoryConnection conn = rep.getConnection();
			
			conn.begin();
			String queryString = "prefix dscrawler: <http://localhost/DSCrawler/> "
					+ "select ?name ?score "
					+ "where {"
					+ " ?uri dscrawler:hasName ?name . "
					+ " ?uri dscrawler:hasScore ?score . "
					+ "} "
					+ "order by desc(?score) limit "+limit_;
			TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = tupleQuery.evaluate();
			
			DefaultCategoryDataset ds = new DefaultCategoryDataset();
			
			try {
				int cont=0;
	            while (result.hasNext()) {  // iterate over the result
					BindingSet bindingSet = result.next();
					Value name_v = bindingSet.getValue("name");
					Value score_v = bindingSet.getValue("score");
					
					String name = name_v.toString().replace("^^<http://www.w3.org/2001/XMLSchema#string>", "");
					String score_=score_v.toString().replace("^^<http://www.w3.org/2001/XMLSchema#double>", "").replace("\"", "");
					Double score = Double.parseDouble(score_);
					cont++;
					ds.addValue(score, "Scores",""+cont);
					
	            }
			}
			finally {
				result.close();
			}
			
			conn.close();
			rep.shutDown();
			
			JFrame frame = new JFrame ("Graphic of the datasets score");
			frame.setDefaultCloseOperation (JFrame.DISPOSE_ON_CLOSE);
			frame.getContentPane().add (new Chart_panel().mount_chart_panel(ds));
			frame.pack();
			frame.setVisible (true);
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
	
	class Chart_panel {
		public JPanel mount_chart_panel(DefaultCategoryDataset ds){
			JPanel jp = new JPanel();
			JFreeChart chart = ChartFactory.createLineChart(
		            "Evolution of datasets score", "Datasets", "Scores",
		            ds, PlotOrientation.VERTICAL, true, true, false);
			final CategoryPlot plot = (CategoryPlot) chart.getPlot();
			
	        plot.setBackgroundPaint(Color.lightGray);
	        plot.setRangeGridlinePaint(Color.white);
	        
	        final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
	        renderer.setSeriesPaint(0, Color.blue);
	        
			ChartPanel cp = new ChartPanel(chart);
			jp.add(cp); 
			return jp;
		}
	}
	
	public void feed_results_table(String path){
		Integer limit=15;
		if(!top_k.getText().equalsIgnoreCase("")){
			limit = Integer.parseInt(top_k.getText());
		}
		File file = new File(path+FileSystems.getDefault().getSeparator()+"rdf_database");
		Repository rep = new SailRepository(new NativeStore(file));
		try {
			rep.initialize();
			RepositoryConnection conn = rep.getConnection();
			
			conn.begin();
			String queryString = "prefix dscrawler: <http://localhost/DSCrawler/> "
					+ " PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "
					+ "select ?name ?score "
					+ "where {"
					+ " ?uri dscrawler:hasName ?name . "
					+ " ?uri dscrawler:hasScore ?score . "
					+ "} "
					+ "order by desc(?score) limit "+limit;
			
			TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);

			TupleQueryResult result = tupleQuery.evaluate();
			try {
				int registers=0;
	            while (result.hasNext()) {  // iterate over the result
					BindingSet bindingSet = result.next();
					Value name = bindingSet.getValue("name");
					Value score = bindingSet.getValue("score");
					
					if(registers==0){
						model_results.setRowCount(0);
					}
					
					model_results.addRow(new Object[]{name.toString().replace("^^<http://www.w3.org/2001/XMLSchema#string>", ""), score.toString().replace("^^<http://www.w3.org/2001/XMLSchema#double>", "")});
					registers++;
	            }
	            if(registers>0){
	            	see_score_graph.setEnabled(true);
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
	
	public void recover_seeds_configuration(){
		File fXmlFile = new File(application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_1"+FileSystems.getDefault().getSeparator().toString()+"current_seeds.xml");
		DocumentBuilderFactory datasets = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = datasets.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("seed");
			for (int temp = 0; temp < nList.getLength(); temp++) {
				Node nNode = nList.item(temp);
						
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					String url =  eElement.getElementsByTagName("url").item(0).getTextContent();
					String structure = eElement.getElementsByTagName("structure").item(0).getTextContent();
					String pages = eElement.getElementsByTagName("pagination").item(0).getTextContent();
					model.addRow(new Object[]{url, structure, pages});
				}
			}
			log_info.append("Configuration file of seeds was loaded.\n");
		} 
		catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			
		} 
		catch (SAXException e) {
			// TODO Auto-generated catch block
			
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			log_info.setText("Configuration file of seeds was not found.\n");
		}
	}
	
	public void generate_config_seeds_file(){
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder ;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("navigation");
			doc.appendChild(rootElement);
			
			Integer l_count = table.getRowCount();
			
			for (int i=0; i<l_count; i++){
				// Seed elements
				Element seed = doc.createElement("seed");
					
				Element url_ = doc.createElement("url");
				url_.appendChild(doc.createTextNode(table.getValueAt(i, 0).toString()));
				seed.appendChild(url_);
				
				Element structure_ = doc.createElement("structure");
				structure_.appendChild(doc.createTextNode(table.getValueAt(i, 1).toString()));
				seed.appendChild(structure_);
				
				Element pagination_ = doc.createElement("pagination");
				pagination_.appendChild(doc.createTextNode(table.getValueAt(i, 2).toString()));
				seed.appendChild(pagination_);
				
				rootElement.appendChild(seed);
			}
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_1"+FileSystems.getDefault().getSeparator().toString()+"current_seeds.xml"));
			transformer.transform(source, result);
		} 
		catch (ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		catch (TransformerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		Repository rep;
		RepositoryConnection conn;
		File file = new File("rdf_database_teste");
		// Enable the construction of a new database to receive the possible new datasets and their respective scores 
		
		rep = new SailRepository(new NativeStore(file));
		try {
			// create an empty model
			Model model = RDFDataMgr.loadModel("result_datahub.ttl");
			
			rep.initialize();
			ValueFactory f  = rep.getValueFactory();
			conn = rep.getConnection();
			conn.begin();
			String namespace = "http://localhost/DSCrawler/";
			
			StmtIterator iter = model.listStatements();
			while (iter.hasNext()) {
			    Statement stmt     = iter.nextStatement();  // get next statement
			    Resource  s   = stmt.getSubject();     // get the subject
			    Property  p = stmt.getPredicate();   // get the predicate
			    
			    URI subject = f.createURI(namespace, s.getLocalName());
				URI predicate = f.createURI(namespace, p.getLocalName());
				if(stmt.getObject().isLiteral()){
			    	Literal obj_2=null;
			    	if(stmt.getObject().asLiteral().getDatatypeURI().equalsIgnoreCase(XSD.getURI()+"double")){
					    double   object    = stmt.getObject().asLiteral().getDouble();      // get the object
					    String o = String.format(Locale.US, "%8f", object);
						obj_2 = f.createLiteral(o, XMLSchema.DOUBLE);
					}
			    	if(stmt.getObject().asLiteral().getDatatypeURI().equalsIgnoreCase(XSD.getURI()+"string")){
					    obj_2 = f.createLiteral(stmt.getObject().asLiteral().getString(), XMLSchema.STRING);
					}
			    	
					conn.add(subject, predicate, obj_2);
			    }
			    else{
			    	URI obj_2 = f.createURI(stmt.getObject().asResource().getURI());
			    	
					conn.add(subject, predicate, obj_2);
			    }
			}
			conn.commit();
			conn.close();
		}
		catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class SimpleHeaderRenderer extends JLabel implements TableCellRenderer{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SimpleHeaderRenderer(){
		setFont(new Font("Times New Roman", Font.BOLD, 14));
	    setBorder(BorderFactory.createEtchedBorder());
	    setHorizontalAlignment( JLabel.CENTER );
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		setText(value.toString());
        return this;
	}
}