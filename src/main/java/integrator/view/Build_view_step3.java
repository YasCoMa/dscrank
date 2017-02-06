package integrator.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Build_view_step3 {
	public static String url_api="http://127.0.0.1/crowdsourcing-validation/api.php?";
	//public static String url_api="http://ypublish.info/crowdsourcing-validation/api.php?";
	public static String id_supervisor="0";
	public static String id_project="0";
	public static String crowd_pass="";
	public static ObjectMapper mapper = new ObjectMapper();
	
	private JTextField name_sup_;
	private JTextField email_sup_;
	private JPasswordField pass_sup_;
	private JTextField name_proj_;
	private JButton insert_sup_proj;
	private JButton change_sup;
	private JButton change_proj;
	private JTextArea description_proj;
	
	public static JLabel n_pairs_;
	static DefaultComboBoxModel<String> model_combo;
	static JComboBox<String> subs;
	private JTextField n_pairs_by_group;
	private JTextField n_groups;
	private JPasswordField crowd_pass_f;
	private JButton conf_new_sub;
	
	private JTextField email_crowd;
	private JTextField name;
	private JButton add_user;
	
	private JLabel info_selection_crowd;
	private JTable table_crowd;
	private DefaultTableModel model_crowd = new DefaultTableModel();
	private JTable table_groups;
	private DefaultTableModel model_groups = new DefaultTableModel();
	private JButton associate;
	private JTable table_association;
	private DefaultTableModel model_association = new DefaultTableModel();
	private JButton jp_undo_assoc;
	
	private JButton submit_tasks;
	
	public JPanel join_all_sections(){
		JPanel merge_panels = new JPanel(new BorderLayout());
		
		JPanel up_area = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel initial_config = send_data_supervisor_project();
		JPanel conf_submissions = mount_area_submissions();
		JPanel add_user_crowd = mount_area_add_user();
		up_area.add(initial_config);
		up_area.add(conf_submissions);
		up_area.add(add_user_crowd);
		
		JPanel list_association = mount_listing_association_crowd();
		
		merge_panels.add("North",up_area);
		merge_panels.add("South",list_association);
		
		return merge_panels;
	}
	
	public JPanel send_data_supervisor_project(){
		TitledBorder config = new TitledBorder("Crowdsourcing configuration");
		JPanel sup_proj = new JPanel (new BorderLayout());
		sup_proj.setBorder(config);
		
		JPanel areas = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		TitledBorder t_sup = new TitledBorder("Supervisor");
		JPanel supervisor = new JPanel(new GridLayout(4,1));
		supervisor.setBorder(t_sup);
		JPanel jp_name_sup = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel name_sup = new JLabel("Name: ");
		name_sup_ = new JTextField(10);
		jp_name_sup.add(name_sup);
		jp_name_sup.add(name_sup_);
		
		JPanel jp_email_sup = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel email_sup = new JLabel("E-mail: ");
		email_sup_ = new JTextField(10);
		jp_email_sup.add(email_sup);
		jp_email_sup.add(email_sup_);
		
		JPanel jp_pass_sup = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel pass_sup = new JLabel("Password: ");
		pass_sup_ = new JPasswordField(10);
		jp_pass_sup.add(pass_sup);
		jp_pass_sup.add(pass_sup_);
		
		JPanel action_change_sup = new JPanel(new FlowLayout(FlowLayout.LEFT));
		change_sup = new JButton("Change");
		change_sup.addActionListener(
		    new ActionListener(){
		        public void actionPerformed(ActionEvent e){
		        	String name=name_sup_.getText();
		        	String email=email_sup_.getText();
		        	@SuppressWarnings("deprecation")
					String pass=pass_sup_.getText().toString();
		        	
		        	if(name.equalsIgnoreCase("") || email.equalsIgnoreCase("") || pass.equalsIgnoreCase("")){
		        		JOptionPane.showMessageDialog(null, "All the fields are obrigatory.", "Erro", JOptionPane.ERROR_MESSAGE);
		        	}
		        	else{
		        		String urlParameters="func=4&name="+name+"&email="+email+"&pass="+pass+"&id="+id_supervisor;
		        		JsonNode res=jsonToNode(post(url_api,urlParameters));
		        		String result=res.get("text_response").asText();
		        		if(result.indexOf("success")==-1){
			        		JOptionPane.showMessageDialog(null, result, "Error", JOptionPane.ERROR_MESSAGE);
			        	}
		        		else{
		        			JOptionPane.showMessageDialog(null, result, "Informação", JOptionPane.PLAIN_MESSAGE);
		        			save_crowd_config(name,email,name_proj_.getText(),Integer.parseInt(id_supervisor),Integer.parseInt(id_project));
			        	}
		        	}
		        }
		    }
		);
		change_sup.setVisible(false);
		action_change_sup.add(change_sup);
		supervisor.add(jp_name_sup);
		supervisor.add(jp_email_sup);
		supervisor.add(jp_pass_sup);
		supervisor.add(action_change_sup);
		
		JPanel project = new JPanel(new BorderLayout());
		JPanel jp_name_proj = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel name_proj = new JLabel("Name: ");
		name_proj_ = new JTextField(10);
		jp_name_proj.add(name_proj);
		jp_name_proj.add(name_proj_);
		
		JPanel jp_desc_proj = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel desc_proj = new JLabel("Description: ");
		description_proj = new JTextArea(6, 12);
		JScrollPane jp= new JScrollPane(description_proj, 
				   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jp_desc_proj.add(desc_proj);
		jp_desc_proj.add(jp);
		
		JPanel action_change_proj = new JPanel(new FlowLayout(FlowLayout.LEFT));
		change_proj = new JButton("Change");
		change_proj.addActionListener(
		    new ActionListener(){
		        public void actionPerformed(ActionEvent e){
		        	String name=name_proj_.getText();
		        	String desc_proj=description_proj.getText();
		        	
		        	if(name.equalsIgnoreCase("")){
		        		JOptionPane.showMessageDialog(null, "The field obrigatory.", "Erro", JOptionPane.ERROR_MESSAGE);
		        	}
		        	else{
		        		String urlParameters="func=5&name="+name+"&id="+id_project+"&id_sup="+id_supervisor+"&description="+desc_proj;
		        		String result=post(url_api,urlParameters);
		        		JsonNode r= jsonToNode(result);
		        		String msg = r.get("text_response").asText();
		        		String id = r.get("id_project").asText();
		        		
		        		if(msg.indexOf("success")==-1){
			        		JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
			        	}
		        		else{
		        			JOptionPane.showMessageDialog(null, msg, "Informação", JOptionPane.PLAIN_MESSAGE);
		        			id_project=id;
		        			save_crowd_config(name_sup_.getText(),email_sup_.getText(),name,Integer.parseInt(id_supervisor),Integer.parseInt(id));
			        	}
		        		change_proj.setText("Change");
		        		
		        	}
		        }
		    }
		);
		change_proj.setVisible(false);
		action_change_proj.add(change_proj);
		project.add(jp_name_proj, BorderLayout.NORTH);
		project.add(jp_desc_proj, BorderLayout.SOUTH);
		
		TitledBorder t_proj = new TitledBorder("Project");
		JPanel project_aux = new JPanel(new BorderLayout());
		project_aux.setBorder(t_proj);
		project_aux.add(project, BorderLayout.NORTH);
		project_aux.add(action_change_proj, BorderLayout.SOUTH);
		
		areas.add( supervisor);
		areas.add( project_aux);
		
		JPanel action_insert = new JPanel(new FlowLayout(FlowLayout.LEFT));
		insert_sup_proj = new JButton("Send");
		insert_sup_proj.addActionListener(
		    new ActionListener(){
		        public void actionPerformed(ActionEvent e){
		        	String name=name_sup_.getText();
		        	String email=email_sup_.getText();
		        	@SuppressWarnings("deprecation")
					String pass=pass_sup_.getText().toString();
		        	String name_proj=name_proj_.getText();
		        	String desc_proj=description_proj.getText();
		        	
		        	if(name.equalsIgnoreCase("") || email.equalsIgnoreCase("") || pass.equalsIgnoreCase("") || name_proj.equalsIgnoreCase("")){
		        		JOptionPane.showMessageDialog(null, "All the fields are obrigatory.", "Erro", JOptionPane.ERROR_MESSAGE);
		        	}
		        	else{
		        		String urlParameters="func=3&name_supervisor="+name+"&email="+email+"&pass="+pass+"&name_project="+name_proj+"&description="+desc_proj;
		        		String result=post(url_api,urlParameters);
		        		JsonNode r= jsonToNode(result);
		        		String msg = r.get("text_response").asText();
		        		String id_sup = r.get("id_supervisor").asText();
		        		String id_proj = r.get("id_project").asText();
		        		
		        		if(id_sup.equalsIgnoreCase("0") || id_proj.equalsIgnoreCase("0")){
			        		JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.ERROR_MESSAGE);
			        		if(!id_sup.equalsIgnoreCase("0") && id_proj.equalsIgnoreCase("0")){
			        			change_proj.setText("Add");
			        			name_proj_.setText("");
			        		}
			        	}
		        		else{
		        			JOptionPane.showMessageDialog(null, msg, "Informação", JOptionPane.PLAIN_MESSAGE);
				        }
		        		id_supervisor=id_sup;
		        		id_project=id_proj;
		        		if(!id_sup.equalsIgnoreCase("0")){
		        			String n_proj = (id_project.equalsIgnoreCase("0")) ? "" : name_proj;
		        			save_crowd_config(name,email,n_proj,Integer.parseInt(id_sup),Integer.parseInt(id_proj));
		        			enable_change_data_config();
		        		}
		        		
		        	}
		        	// Receber post
		        }
		    }
		);
		action_insert.add(insert_sup_proj);
		
		sup_proj.add("North", areas);
		sup_proj.add("South", action_insert);
		
		return sup_proj;
	}
	
	public void enable_change_data_config(){
		insert_sup_proj.setVisible(false);
		change_sup.setVisible(true);
		change_proj.setVisible(true);
		
		email_crowd.setEnabled(true);
		name.setEnabled(true);
		add_user.setEnabled(true);
		System.out.println(n_pairs_.getText());
		if(!n_pairs_.getText().equalsIgnoreCase("0")){
			n_groups.setEnabled(true);
			crowd_pass_f.setEnabled(true);
			conf_new_sub.setEnabled(true);
		}
		
	}
	
	public JPanel mount_area_submissions(){
		TitledBorder config = new TitledBorder("Submission configuration");
		JPanel merge = new JPanel(new GridLayout(5,1));
		merge.setBorder(config);
		
		JPanel jp_n_pairs = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel i_n_pairs = new JLabel("Number of pairs to submit: ");
		n_pairs_ = new JLabel("");
		Integer count = (countPairs()==0) ? 0 : (countPairs()-1);
		n_pairs_.setText(count+"");
		jp_n_pairs.add(i_n_pairs);
		jp_n_pairs.add(n_pairs_);
		
		JPanel jp_combo_submissions = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel i_combo_subs = new JLabel("Last Submissions: ");
		model_combo = new DefaultComboBoxModel<String>(populate_combo_submissions());
		subs = new JComboBox<String>(model_combo);
		subs.addItemListener(new ItemChangeListener());
		jp_combo_submissions.add(i_combo_subs);
		jp_combo_submissions.add(subs);
		
		JPanel jp_n_pairs_group = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel i_n_pairs_group = new JLabel("Number of pairs by group: ");
		n_pairs_by_group = new JTextField(4);
		n_pairs_by_group.setEditable(false);
		jp_n_pairs_group.add(i_n_pairs_group);
		jp_n_pairs_group.add(n_pairs_by_group);
		
		JPanel jp_n_groups = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel i_n_groups = new JLabel("Number of groups: ");
		n_groups = new JTextField(4);
		jp_n_groups.add(i_n_groups);
		jp_n_groups.add(n_groups);
		n_groups.setEnabled(false);
		
		JPanel jp_crowd_pass = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel i_crowd_pass = new JLabel("General password for the crowd: ");
		crowd_pass_f = new JPasswordField(10);
		jp_crowd_pass.add(i_crowd_pass);
		jp_crowd_pass.add(crowd_pass_f);
		crowd_pass_f.setEnabled(false);
		JPanel jp_action_conf = new JPanel(new FlowLayout(FlowLayout.LEFT));
		conf_new_sub = new JButton("Configure");
		conf_new_sub.addActionListener(
		    new ActionListener(){
		        @SuppressWarnings("deprecation")
				public void actionPerformed(ActionEvent e){
		        	if(!id_project.equalsIgnoreCase("0")){
			        	if(n_groups.getText().equalsIgnoreCase("")){
			        		JOptionPane.showMessageDialog(null, "You need to specify a number of groups to divide the tasks.", "Error", JOptionPane.ERROR_MESSAGE);
			        	}
			        	else{
			        		try{
			        			reload_area_submission();
			        			Integer num_groups = Integer.parseInt(n_groups.getText());
			        			Integer total_pairs = Integer.parseInt(n_pairs_.getText());
			        			n_pairs_by_group.setText((total_pairs/num_groups)+"");
			        			crowd_pass=crowd_pass_f.getText();
			        			for (int i=1; i<=num_groups; i++){
									model_groups.addRow(new Object[]{"(G"+i+")"});
								}
			        		}
			        		catch(Exception e1){
			        			JOptionPane.showMessageDialog(null, "You did not type an integer number.", "Error", JOptionPane.ERROR_MESSAGE);
			        		}
			        	}
		        	}
		        	else{
		        		JOptionPane.showMessageDialog(null, "You need to configure the project and the supervisor.", "Error", JOptionPane.ERROR_MESSAGE);
		        	}
		        }
		    }
		);
		conf_new_sub.setEnabled(false);
		jp_action_conf.add(conf_new_sub);
		
		merge.add(jp_n_pairs);
		merge.add(jp_combo_submissions);
		merge.add(jp_n_groups);
		merge.add(jp_n_pairs_group);
		merge.add(jp_crowd_pass);
		merge.add(jp_action_conf);
		
		return merge;
	}
	
	public JPanel mount_area_add_user(){
		TitledBorder config = new TitledBorder("Crowd Addition");
		JPanel merge = new JPanel (new GridLayout(3,1));
		merge.setBorder(config);
		
		JPanel jp_email = new JPanel (new FlowLayout(FlowLayout.LEFT));
		JLabel i_email = new JLabel("Email: ");
		email_crowd = new JTextField(10);
		jp_email.add(i_email);
		jp_email.add(email_crowd);
		email_crowd.setEnabled(false);
		
		JPanel jp_name = new JPanel (new FlowLayout(FlowLayout.LEFT));
		JLabel i_name = new JLabel("Name: ");
		name = new JTextField(10);
		jp_name.add(i_name);
		jp_name.add(name);
		name.setEnabled(false);
		
		JPanel jp_action_add = new JPanel (new FlowLayout(FlowLayout.LEFT));
		add_user = new JButton("Add");
		add_user.addActionListener(
		    new ActionListener(){
		        public void actionPerformed(ActionEvent e){
		        	if(email_crowd.getText().equalsIgnoreCase("") || name.getText().equalsIgnoreCase("")){
		        		JOptionPane.showMessageDialog(null, "The fields are obrigatory.", "Error", JOptionPane.ERROR_MESSAGE);
		        	}
		        	else{
		        		String name_ = name.getText();
		        		name_=name_.replace("(", "");
		        		name_=name_.replace(")", "");
		        		name_=name_.replace("#", "");
		        		
		        		String email_ = email_crowd.getText();
		        		JsonNode result = jsonToNode(post(url_api, "func=10&id_user=0&name_user="+name_+"&email_user="+email_+"&id_supervisor="+id_supervisor));
		        		JOptionPane.showMessageDialog(null, result.get("text_response").asText(), "Information", JOptionPane.PLAIN_MESSAGE);
		        		String id_user = result.get("id_user").asText();
		        		model_crowd.addRow(new Object[]{"(#"+id_user+") "+name_});
		        		
		        		name.setText("");
		        		email_crowd.setText("");
		        		associate.setEnabled(true);
		        		// Alimentar a lista do crowd
		        	}
		        }
		    }
		);
		add_user.setEnabled(false);
		jp_action_add.add(add_user);
		
		merge.add(jp_name);
		merge.add(jp_email);
		merge.add(jp_action_add);
		
		return merge;
	}
	
	public JPanel mount_listing_association_crowd(){
		JPanel listing = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		JPanel lists_association = new JPanel(new FlowLayout(FlowLayout.CENTER));
		
		table_crowd = new JTable();
		String[] columnNames_crowd = new String[] {"Crowd"};
        String[][] rowData_crowd = new String[][] {{""}};
		model_crowd = new DefaultTableModel(rowData_crowd, columnNames_crowd) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table_crowd = new JTable();
		table_crowd.setModel(model_crowd);
		table_crowd.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		ListSelectionModel selectionModel = table_crowd.getSelectionModel();
		selectionModel.addListSelectionListener(new ListSelectionListener() {
		    public void valueChanged(ListSelectionEvent e) {
		        info_selection_crowd.setText("Selected: "+table_crowd.getSelectedRowCount()+"/"+table_crowd.getRowCount());
		    }
		});
		model_crowd.removeRow(0);
		table_crowd.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
        JScrollPane p_table = new JScrollPane(table_crowd);
        p_table.setPreferredSize(new Dimension(200, 300));
        p_table.getVerticalScrollBar();
        info_selection_crowd=new JLabel("");
        JPanel crowd_jp = new JPanel(new BorderLayout());
        crowd_jp.add(info_selection_crowd);
        crowd_jp.add(p_table);
        
        JPanel action_crowd = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton clear_selection_crowd = new JButton("Clear selection");
        clear_selection_crowd.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	table_crowd.clearSelection();
	            }
	        }
        );
        JButton jp_delete_crowd = new JButton("Delete");
        jp_delete_crowd.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	int[] selection = table_crowd.getSelectedRows();
	            	if(selection.length==0){
	            		JOptionPane.showMessageDialog(null, "You did not select the user to delete.", "Error", JOptionPane.ERROR_MESSAGE);
	            	}
	            	else{
	            		if(selection.length==1){
	            			String user=model_crowd.getValueAt(selection[0], 0).toString();
	            			Integer id_user=Integer.parseInt(user.substring(user.indexOf("(#")+2, user.indexOf(")")));
			            	String urlParameters = "func=14&id="+id_user;
			        	    JsonNode resp = jsonToNode(post(url_api,urlParameters));
			        	    if(resp.get("text_response").asText().indexOf("success")!=-1){
			        	    	JOptionPane.showMessageDialog(null, resp.get("text_response").asText(), "Error", JOptionPane.ERROR_MESSAGE);
			        	    }
			        	    else{
			        	    	JOptionPane.showMessageDialog(null, resp.get("text_response").asText(), "Information", JOptionPane.PLAIN_MESSAGE);
			        	    	model_crowd.removeRow(selection[0]);
			        	    }
	            		}
	            		else{
	            			JOptionPane.showMessageDialog(null, "You have to select just one user to delete.", "Error", JOptionPane.ERROR_MESSAGE);
	            		}
	            	}
	            }
	        }
        );
        action_crowd.add(clear_selection_crowd);
        action_crowd.add(jp_delete_crowd);
        
        JPanel jp_table_crowd = new JPanel(new BorderLayout());
		jp_table_crowd.add("North", crowd_jp);
		jp_table_crowd.add("South", action_crowd);
		
        table_groups = new JTable();
		String[] columnNames_groups = new String[] {"Groups"};
        String[][] rowData_groups = new String[][] {{""}};
		model_groups = new DefaultTableModel(rowData_groups, columnNames_groups) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table_groups = new JTable();
		table_groups.setModel(model_groups);
		table_groups.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		model_groups.removeRow(0);
		table_groups.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
        JScrollPane p_table_group = new JScrollPane(table_groups);
        p_table_group.setPreferredSize(new Dimension(200, 300));
        p_table_group.getVerticalScrollBar();
        
        associate = new JButton("Associate");
		associate.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	int[] list_crowd = table_crowd.getSelectedRows();
	            	int[] group = table_groups.getSelectedRows();
	            	table_crowd.clearSelection();
	            	
	            	if(list_crowd.length==0 || group.length==0){
	            		JOptionPane.showMessageDialog(null, "You have to select at least one person from the crowd and one group.", "Error", JOptionPane.ERROR_MESSAGE);
	            	}
	            	else{
	            		String group_name=model_groups.getValueAt(group[0], 0).toString();
		            	for (int i:list_crowd){
		            		String name=model_crowd.getValueAt(i, 0).toString();
	            			model_association.addRow(new Object[]{group_name+"-"+name});
	            		}
		            	
		            	int remov=0;
		            	for (int i:list_crowd){
	            			model_crowd.removeRow(i-remov);
	            			remov++;
	            		}
	            	}
	            }
	        }
		);
        associate.setEnabled(false);
        
        table_association = new JTable();
		String[] columnNames_association = new String[] {"Associations"};
        String[][] rowData_association = new String[][] {{""}};
		model_association = new DefaultTableModel(rowData_association, columnNames_association) {
            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table_association = new JTable();
		table_association.setModel(model_association);
		table_association.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		model_association.removeRow(0);
		table_association.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
        JScrollPane p_table_association = new JScrollPane(table_association);
        p_table_association.setPreferredSize(new Dimension(200, 300));
        p_table_association.getVerticalScrollBar();
        
        JPanel action_association = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton clear_selection_association = new JButton("Clear selection");
        clear_selection_crowd.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	table_association.clearSelection();
	            }
	        }
        );
        jp_undo_assoc = new JButton("Undo association");
        jp_undo_assoc.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	int[] selection = table_association.getSelectedRows();
	            	if(selection.length==0){
	            		JOptionPane.showMessageDialog(null, "You did not select the user to delete.", "Error", JOptionPane.ERROR_MESSAGE);
	            	}
	            	else{
	            		for (int i=0;i<selection.length;i++){
	            			String value=model_association.getValueAt(selection[i], 0).toString();
	            			model_crowd.addRow(new Object[]{value.split("-")[1]});
	            			model_association.removeRow(selection[i]);
	            		}
	            			
	            	}
	            }
	        }
        );
        action_association.add(clear_selection_association);
        action_association.add(jp_undo_assoc);
        
        JPanel jp_table_association = new JPanel(new BorderLayout());
		jp_table_association.add("North", p_table_association);
		jp_table_association.add("South", action_association);
        
        lists_association.add(jp_table_crowd);
        lists_association.add(p_table_group);
        lists_association.add(associate);
        lists_association.add(jp_table_association);
        listing.add(lists_association);
		
        JPanel action_submit = new JPanel(new FlowLayout(FlowLayout.LEFT));
        submit_tasks = new JButton("Submit tasks");
        submit_tasks.addActionListener(
		    new ActionListener(){
	            public void actionPerformed(ActionEvent e){
	            	JDialog.setDefaultLookAndFeelDecorated(true);
	                int response = JOptionPane.showConfirmDialog(null, "Do you want to continue?", "Confirmation",
	                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	                if (response == JOptionPane.YES_OPTION) {
	                	int cont_group_assoc=0;
		            	for (int j=0;j<model_groups.getRowCount();j++){
		            		for(int i=0;i<model_association.getRowCount();i++){
			            		if(model_association.getValueAt(i, 0).toString().indexOf(model_groups.getValueAt(j, 0).toString())!=-1){
			            			cont_group_assoc++;
			            			break;
			            		}
			            	}
		            	}
		            	boolean ok_groups=false;
		            	if(cont_group_assoc==model_groups.getRowCount()){
		            		ok_groups=true;
		            	}
		            	if(!ok_groups){
		            		JOptionPane.showMessageDialog(null, "You have to associate users for all the groups.", "Error", JOptionPane.ERROR_MESSAGE);
		            	}
		            	else{
		            		submit_tasks.setEnabled(false);
		            		
		            		String num_pairs=n_pairs_.getText();
		            		String num_groups=n_groups.getText();
		            		String urlParameters="func=7&id_supervisor="+id_supervisor+"&id_project="+id_project+"&n_pairs="+num_pairs+"&n_groups="+num_groups+"&pass="+crowd_pass;
		        			String result=post(url_api,urlParameters);
		        			JsonNode subm= jsonToNode(result);
		        			String id_submission=subm.get("id_submission").asText();
		        			
		        			int well_associations=0;
		        			int n_rows = model_association.getRowCount();
		        			for (int i=0;i<n_rows;i++){
		        				String group = model_association.getValueAt(i, 0).toString().split("-")[0];
		        				group=group.substring(group.indexOf("(G")+2, group.indexOf(")"));
		        				
		        				String user = model_association.getValueAt(i, 0).toString().split("-")[1];
		        				user=user.substring(user.indexOf("(#")+2, user.indexOf(")"));
		        				
		        				urlParameters="func=15&n_group="+group+"&id_submission="+id_submission+"&id_user="+user;
			        			result=post(url_api,urlParameters);
			        			//JsonNode assoc= jsonToNode(result);
			        			//if(assoc.get("text_response").asText().indexOf("success")!=-1){
			        				well_associations++;
			        			//}
			        			System.out.println(result);
		        			}
		        			
		        			submit_tasks(id_submission);
		        			
		        			Build_view_step3.model_combo = new DefaultComboBoxModel<String>(populate_combo_submissions());
		        			subs.setModel(model_combo);
		        			
		        			submit_tasks.setEnabled(true);
		        			
		        			JOptionPane.showMessageDialog(null, "Tasks were published.", "Information", JOptionPane.PLAIN_MESSAGE);
		            	}
	                }
	                else{
	                	JOptionPane.showMessageDialog(null, "Ok, Nothing was saved.", "Information", JOptionPane.INFORMATION_MESSAGE);
	                }
	            }
	        }
		);
        action_submit.add(submit_tasks);
        
        JPanel association_submit = new JPanel(new BorderLayout());
        association_submit.add("North", listing);
        association_submit.add("South", action_submit);
		
		return association_submit;
	}
	
	public void submit_tasks(String id_submission){
		String csvFile = Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"result_metrics.csv";
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		
		String path=Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish";
		
		int cont=0;
		try {
			br = new BufferedReader(new FileReader(csvFile));
			while ((line = br.readLine()) != null) {
				if(cont!=0){
					String nodes = "[";
					String edges = "[";
					
					String[] features = line.split(cvsSplitBy);
					
					String url=features[0];
					String type = (url.indexOf("sparql")!=-1) ? "endpoint" : "file" ;
					String uri=features[3];
					
					ArrayList<ArrayList<String>> info_source = mount_query_features_for_localSource(features[1], path);
					ArrayList<ArrayList<String>> info = mount_query_features(url, type, uri, path);
					
					nodes+="{ id: 0, label: \""+features[1]+"\", x: 340, y: 60, group: 0 }, ";
					nodes+="{ id: 1, label: \""+features[2]+"\", x: 340, y: 100, group: 0 }, ";
					nodes+="{ id: 3, label: \""+features[3]+"\", x: 420, y: 140, group: 1 }, ";
					nodes+="{ id: 4, label: \""+features[4]+"\", x: 420, y: 180, group: 1 } ";
					
					if(info.get(0).size()>0){
						nodes+=", { id: 5, label: \""+info.get(0).get(0)+"\", x: 380, y: 220, group: 1 } ";
					}
					if(info.get(1).size()==1){
						nodes+=", { id: 6, label: \""+info.get(1).get(0)+"\", x: 420, y: 220, group: 1 } ";
					}
					if(info.get(1).size()>1){
						nodes+=", { id: 6, label: \""+info.get(1).get(0)+"\", x: 380, y: 220, group: 1 } ";
						nodes+=", { id: 7, label: \""+info.get(1).get(1)+"\", x: 420, y: 300, group: 1 } ";
					}
					
					if(info_source.get(0).size()>0){
						nodes+=", { id: 8, label: \""+info_source.get(0).get(0)+"\", x: 280, y: 220, group: 0 } ";
					}
					if(info_source.get(1).size()==1){
						nodes+=", { id: 9, label: \""+info_source.get(1).get(0)+"\", x: 280, y: 220, group: 0 } ";
					}
					if(info_source.get(1).size()>1){
						nodes+=", { id: 9, label: \""+info_source.get(1).get(0)+"\", x: 280, y: 260, group: 0 } ";
						nodes+=", { id: 10, label: \""+info_source.get(1).get(1)+"\", x: 360, y: 340, group: 0 } ";
					}
					
					edges+="{ from: 0, to: 1, label: \"label\" }, ";
					edges+="{ from: 3, to: 4, label: \"label\" }, ";
					edges+="{ from: 0, to: 3, label: \"?\" } ";
					
					if(info.get(0).size()>0){
						edges+=", { from: 3, to: 5, label: \"description\" } ";
					}
					if(info.get(1).size()==1){
						edges+=", { from: 3, to: 6, label: \"class\" } ";
					}
					if(info.get(1).size()>1){
						edges+=", { from: 3, to: 6, label: \"class\" } ";
						edges+=", { from: 3, to: 7, label: \"class\" }";
					}
					
					if(info_source.get(0).size()>0){
						edges+=", { from: 0, to: 8, label: \"description\" } ";
					}
					if(info_source.get(1).size()==1){
						edges+=", { from: 0, to: 9, label: \"class\" } ";
					}
					if(info_source.get(1).size()>1){
						edges+=", { from: 0, to: 9, label: \"class\" } ";
						edges+=", { from: 0, to: 10, label: \"class\" }";
					}
					
					nodes += "]";
					edges += "]";
					
					String type_target = "resource";
					if(verify_instance_owlClass(info.get(3)) || info.get(2).size()>0){
						type_target="class";
					}
					
					String type_source = "resource";
					if(verify_instance_owlClass(info_source.get(3)) || info_source.get(2).size()>0){
						type_source="class";
					}
					
					String urlParameters="func=2&id_submission="+id_submission+"&uri_source="+features[1]+"&uri_target="+features[3]+"&type_source="+type_source+"&type_target="+type_target+"&id_submission="+id_submission+"&nodes="+(nodes+"-"+edges);
	    			String result=post(url_api,urlParameters);
	    			//JsonNode assoc= jsonToNode(result);
	    			System.out.println(result);
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
		
	}
	
	public boolean verify_instance_owlClass(ArrayList<String> l){
		boolean r = false;
		for(String l_: l){
			if(l_.contains("https://www.w3.org/2002/07/owl#Class") || l_.contains("http://www.w3.org/2000/01/rdf-schema#Class")){
				r=true;
			}
		}
		return r;
	}
	public ArrayList<ArrayList<String>> mount_query_features(String url, String type, String uri, String path){
		ArrayList<ArrayList<String>> info = new ArrayList<ArrayList<String>>();
		
		url=url.replace("\"", "");
		if(!url.startsWith("http://")){
			url="http://"+url;
		}
		//String path_from_local = new File(path+FileSystems.getDefault().getSeparator()+"data_publishing_dataset.rdf").getAbsolutePath();
		String from_named="";
		String consult_target="";
		String lim ="", internal=" "
				+ " { <"+uri+">  rdfs:label  ?label . } "
				+ " optional { <"+uri+">  rdf:type  ?class . } "
				+ " optional { <"+uri+">  rdfs:subClassOf  ?superclass . } "
				+ " optional { ?instance rdf:type <"+uri+"> . } "
				+ " optional { <"+uri+">  ?p  ?description . filter contains(str(?p), 'descr') . } ";
		if(type.equalsIgnoreCase("endpoint")){
			from_named="";
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
									+ " { <"+uri+">  rdfs:label  ?label . } "
									+ " optional { <"+uri+">  rdf:type  ?class . } "
									+ " optional { <"+uri+">  rdfs:subClassOf  ?superclass . } "
									+ " optional { ?instance rdf:type <"+uri+"> . } "
									+ " optional { <"+uri+">  ?p  ?description . filter contains(str(?p), 'descr') . } "
					+ "				    } ";
				url_query="http://virtuoso.openlifedata.org/sparql";
			}
			consult_target="service <"+url_query+"> ";
			
		}
		else{
			consult_target="graph <"+url+"> ";
			from_named="from named <"+url+"> ";
		}
		// Ajustar o from local e colocar de acordo com o caminho do workspace do usuário
		String sparqlQuery = ""
				+ "PREFIX isparql:  <java:integrator.auto_mapping.isparqlProperties.> "
				+ "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#> "
				+ "PREFIX owl:     <http://www.w3.org/2002/07/owl#> "
				+ ""
				+ "SELECT distinct ?label ?description ?class ?superclass ?instance "
				+ "from <file:///"+(path+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()).replace("\\", "/")+"data_publishing_dataset.rdf> "
				+ from_named
				+ "WHERE {"
				//+ "    { "
				+ "        select * where { "
				+ "            "+consult_target+" { "
				+					internal
				+ "            } "
				+ "        } "
				//+ "    } "
				+ ""
				+ "} ";
		
		ArrayList<String> label = new ArrayList<String>();
		ArrayList<String> description = new ArrayList<String>();
		ArrayList<String> class_ = new ArrayList<String>();
		ArrayList<String> super_class_ = new ArrayList<String>();
		ArrayList<String> instance = new ArrayList<String>();
		
		System.out.println(sparqlQuery);
		Query query_result = QueryFactory.create(sparqlQuery);
		try{
			QueryExecution qexec_result = QueryExecutionFactory.create(query_result);
			qexec_result.setTimeout(600000);
			try{
				ResultSet results = qexec_result.execSelect();
				while (results.hasNext()) {
				  	QuerySolution solution = results.next();
				  	String l=solution.get("label").toString();
				  	System.out.println(l);
				  	if(!label.contains(l)){
				  		label.add(l);
				  		if(solution.get("description")!=null){
				  			String desc = solution.get("description").toString();
				  			if(!description.contains(desc)){
				  				description.add(desc);
				  			}
				  		}
				  		
				  		if(solution.get("superclass")!=null){
				  			String sc = solution.get("superclass").toString();
				  			if(!super_class_.contains(sc)){
				  				super_class_.add(sc);
				  			}
				  		}
				  		
				  	}
				  	
				  	if(solution.get("instance")!=null){
			  			String ins = solution.get("instance").toString();
			  			if(!instance.contains(ins)){
			  				instance.add(ins);
			  			}
			  		}
				  	
				  	if(solution.get("class")!=null){
					  	String classe = solution.get("class").toString();
					  	class_.add(classe);
				  	}
				}
				
			}
			catch(org.apache.jena.sparql.resultset.ResultSetException ee1){
				System.out.println("Query with incorrect answer or file with wrong type.");
			}
			catch(HttpException ee3){
				System.out.println("URL could not receiving a query.");
			}
			catch(Exception er){
				
			}
			finally{
				qexec_result.close();
			}
		}
		catch(org.apache.jena.query.QueryExecException ee2){
			System.out.println("URL is invalid and the query cannot be continued.");
		}
		
		info.add(description);
		info.add(class_);
		info.add(super_class_);
		info.add(instance);
		
		return info;
	}
	
	public ArrayList<ArrayList<String>> mount_query_features_for_localSource(String uri, String path){
		ArrayList<ArrayList<String>> info = new ArrayList<ArrayList<String>>();
		
		// Ajustar o from local e colocar de acordo com o caminho do workspace do usuário
		String sparqlQuery = ""
				+ "PREFIX isparql:  <java:integrator.auto_mapping.isparqlProperties.> "
				+ "PREFIX rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
				+ "PREFIX rdfs:     <http://www.w3.org/2000/01/rdf-schema#> "
				+ "PREFIX owl:     <http://www.w3.org/2002/07/owl#> "
				+ ""
				+ "SELECT distinct ?label ?description ?class ?superclass ?instance "
				+ "from <file:///"+(path+FileSystems.getDefault().getSeparator()+"step_1"+FileSystems.getDefault().getSeparator()).replace("\\", "/")+"data_publishing_dataset.rdf> "
				+ "WHERE {"
						+ " { <"+uri+">  rdfs:label  ?label . } "
				+ " optional { <"+uri+">  rdf:type  ?class . } "
				+ " optional { <"+uri+">  rdfs:subClassOf  ?superclass . } "
				+ " optional { ?instance rdf:type <"+uri+"> . } "
				+ " optional { <"+uri+">  ?p  ?description . filter contains(str(?p), 'descr') . } "
				+ ""
				+ "} ";
		
		ArrayList<String> label = new ArrayList<String>();
		ArrayList<String> description = new ArrayList<String>();
		ArrayList<String> class_ = new ArrayList<String>();
		ArrayList<String> super_class_ = new ArrayList<String>();
		ArrayList<String> instance = new ArrayList<String>();
		
		System.out.println(sparqlQuery);
		Query query_result = QueryFactory.create(sparqlQuery);
		try{
			QueryExecution qexec_result = QueryExecutionFactory.create(query_result);
			try{
				ResultSet results = qexec_result.execSelect();
				while (results.hasNext()) {
				  	QuerySolution solution = results.next();
				  	String l=solution.get("label").toString();
				  	System.out.println(l);
				  	if(!label.contains(l)){
				  		label.add(l);
				  		if(solution.get("description")!=null){
				  			String desc = solution.get("description").toString();
				  			if(!description.contains(desc)){
				  				description.add(desc);
				  			}
				  		}
				  		
				  		if(solution.get("superclass")!=null){
				  			String sc = solution.get("superclass").toString();
				  			if(!super_class_.contains(sc)){
				  				super_class_.add(sc);
				  			}
				  		}
				  		
				  	}
				  	
				  	if(solution.get("instance")!=null){
			  			String ins = solution.get("instance").toString();
			  			if(!instance.contains(ins)){
			  				instance.add(ins);
			  			}
			  		}
				  	
				  	if(solution.get("class")!=null){
					  	String classe = solution.get("class").toString();
					  	class_.add(classe);
				  	}
				}
				
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
		
		info.add(description);
		info.add(class_);
		info.add(super_class_);
		info.add(instance);
		
		return info;
	}
	
	public Vector<String> populate_combo_submissions(){
		Vector<String> items=new Vector<String>();
		items.add("New submission");
		
		if(!id_supervisor.equalsIgnoreCase("0") && !id_project.equalsIgnoreCase("0")){
			String urlParameters="func=8&id_supervisor="+id_supervisor+"&id_project="+id_project;
			String result=post(url_api,urlParameters).replace("ï»¿","");
			JsonNode submissions= jsonToNode(result);
			if(submissions!=null){
				for(JsonNode sub:submissions){
					items.add(sub.get("date").asText());
				}
				save_last_submissions(submissions);
			}
		}
		
		return items;
	}
	
	class ItemChangeListener implements ItemListener{
	    public void itemStateChanged(ItemEvent event) {
	        if (event.getStateChange() == ItemEvent.SELECTED) {
	            Object item = event.getItem();
	            if(!item.toString().equalsIgnoreCase("New submission")){
	            	
	            	reload_area_submission();
	        	    Integer id=load_data_submission(item.toString());
	        	    conf_new_sub.setEnabled(false);
	        	    associate.setEnabled(false);
	        	    submit_tasks.setEnabled(false);
	        	    jp_undo_assoc.setEnabled(false);
	        	    
	        	    String urlParameters = "func=9&id_submission="+id;
	        	    JsonNode assocs = jsonToNode(post(url_api,urlParameters));
	        	    for (JsonNode asc:assocs){
	        	    	model_association.addRow(new Object[]{"(G"+asc.get("number_group").asText()+") - "+asc.get("name").asText()});
	        	    }
	        	    
	        	    //desabilita botão de associação
	            }
	            else{
	        	    conf_new_sub.setEnabled(true);
	        	    associate.setEnabled(true);
	        	    submit_tasks.setEnabled(true);
	        	    jp_undo_assoc.setEnabled(true);
	        	    
	        	    Integer count = (countPairs()==0) ? 0 : countPairs()-1;
	        	    n_pairs_.setText(count+"");
	        	    n_pairs_by_group.setText("");
	        	    n_groups.setText("");
	        	    crowd_pass_f.setText("");
	        	    
	        	    reload_area_submission();
	        	    //habilita botão de associação e reseta o de número de grupos e reconta os pares
	            }
	        }
	    }       
	}
	
	public void reload_area_submission(){
		model_groups.setRowCount(0);
	    model_association.setRowCount(0);;
	    model_crowd.setRowCount(0);
	    
	    String urlParameters = "func=11&id_supervisor="+id_supervisor;
	    JsonNode users = jsonToNode(post(url_api,urlParameters));
	    int c=0; 
	    for (JsonNode usr:users){
	    	model_crowd.addRow(new Object[]{"(#"+usr.get("id").asText()+") "+usr.get("name").asText()});
	    	c++;
	    }
	    if(c>0){
	    	associate.setEnabled(true);
	    }
	}
	
	public Integer load_data_submission(String in_date){
		int id=0;
		File fXmlFile = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_3"+FileSystems.getDefault().getSeparator().toString()+"crowd_project_submissions.xml");
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
					
					String date = eElement.getElementsByTagName("date").item(0).getTextContent();
					if(date.equalsIgnoreCase(in_date)){
						id=Integer.parseInt(eElement.getAttribute("id"));
						
						Integer total_pairs = Integer.parseInt(eElement.getElementsByTagName("number_of_pairs").item(0).getTextContent()); 
						n_pairs_.setText(total_pairs+"");
						Integer number_groups = Integer.parseInt(eElement.getElementsByTagName("number_of_groups").item(0).getTextContent()); 
						n_groups.setText(number_groups+"");
						Integer pairs_by_group = total_pairs/number_groups;
						n_pairs_by_group.setText(pairs_by_group+"");
						for (int i=1; i<=number_groups; i++){
							model_groups.addRow(new Object[]{"(G"+i+")"});
						}
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
			File fg = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_3"+FileSystems.getDefault().getSeparator().toString()+"crowd_project_submissions.xml");
			if(!fg.exists()){
				fg.createNewFile();
			}
			StreamResult result = new StreamResult(fg);
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
	
	public void read_crowd_config(String mode){
		File fXmlFile = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_3"+FileSystems.getDefault().getSeparator().toString()+"config_crowdsourcing.xml");
		DocumentBuilderFactory datasets = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = datasets.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			Node nNode = doc.getElementsByTagName("crowd_config").item(0);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				
				if(mode.equalsIgnoreCase("step_3")){
					String name = eElement.getElementsByTagName("name_supervisor").item(0).getTextContent();
					name_sup_.setText(name);
					String email = eElement.getElementsByTagName("email_supervisor").item(0).getTextContent();
					email_sup_.setText(email);
					String name_proj = eElement.getElementsByTagName("name_project").item(0).getTextContent();
					name_proj_.setText(name_proj);
				}
				
				String id_sup = eElement.getElementsByTagName("id_supervisor").item(0).getTextContent();
				id_supervisor=id_sup;
				String id_proj = eElement.getElementsByTagName("id_project").item(0).getTextContent();
				id_project=id_proj;
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
	}
	
	public void save_crowd_config(String name_sup, String email, String name_proj, Integer id_sup, Integer id_proj){
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder ;
		
		try {
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("crowd_config");
			doc.appendChild(rootElement);
			
			Element el_name_sup = doc.createElement("name_supervisor");
			el_name_sup.appendChild(doc.createTextNode(name_sup));
			rootElement.appendChild(el_name_sup);
			
			Element el_email_sup = doc.createElement("email_supervisor");
			el_email_sup.appendChild(doc.createTextNode(email));
			rootElement.appendChild(el_email_sup);
			
			Element el_id_sup = doc.createElement("id_supervisor");
			el_id_sup.appendChild(doc.createTextNode(id_sup+""));
			rootElement.appendChild(el_id_sup);
			
			Element el_name_proj = doc.createElement("name_project");
			el_name_proj.appendChild(doc.createTextNode(name_proj));
			rootElement.appendChild(el_name_proj);
			
			Element el_id_proj = doc.createElement("id_project");
			el_id_proj.appendChild(doc.createTextNode(id_proj+""));
			rootElement.appendChild(el_id_proj);
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			
			File dir_step3 = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_3");
        	if(!dir_step3.exists())
        		dir_step3.mkdir();
        	File fg = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_3"+FileSystems.getDefault().getSeparator().toString()+"config_crowdsourcing.xml");
        	if(!fg.exists()){
				fg.createNewFile();
			}
			StreamResult result = new StreamResult(fg);
			transformer.transform(source, result);
			
			System.out.println("Crowdsourcing configuration saved!");
			   
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
	
	public static int countPairs() {
	    InputStream is;
	    int count = 0;
        try {
	    	is = new BufferedInputStream(new FileInputStream(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator()+"step_2"+FileSystems.getDefault().getSeparator()+"result_metrics.csv"));
	        byte[] c = new byte[1024];
	        int readChars = 0;
	        boolean empty = true;
	        while ((readChars = is.read(c)) != -1) {
	            empty = false;
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\n') {
	                    ++count;
	                }
	            }
	        }
	        is.close();
	        count= (count == 0 && !empty) ? 1 : count;
	    } 
	    catch (FileNotFoundException e) {
	    	count=0;
		} 
	    catch (IOException e) {
			count=0;
		}
	    return count;
	}
	
	public String post(String urlToGet, String urlParameters) {
        URL url;
        HttpURLConnection conn;

        String line;
        String result = "";
        try {
            url = new URL(urlToGet);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setReadTimeout(600000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            
            BufferedReader rd = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            while ((line = rd.readLine()) != null) {
                result += line;
            }
            result=result.replace("ï»¿", "");
            System.out.println(result);
            rd.close();
            
            conn.disconnect();

        } 
        catch(FileNotFoundException ef){
        	System.out.println("erro[Url não encontrada!]");
        }
        catch (Exception e) {
            System.out.println("erro[resposta http errada] "+e.getMessage());
        }

        return result;
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
		Build_view_step3 s = new Build_view_step3();
		String path = "C:"+FileSystems.getDefault().getSeparator()+"Users"+FileSystems.getDefault().getSeparator()+"QBEX_PC"+FileSystems.getDefault().getSeparator()+"OneDrive"+FileSystems.getDefault().getSeparator()+"mestrado"+FileSystems.getDefault().getSeparator()+"first_phase";
		s.mount_query_features("http://wifo5-04.informatik.uni-mannheim.de/drugbank/all/targets", "file", "http://wifo5-04.informatik.uni-mannheim.de/drugbank/resource/targets/2603", path);
		s.mount_query_features_for_localSource("http://www.bioknowlogy.br/resource/Classe/ClassA", path);
	}
}
