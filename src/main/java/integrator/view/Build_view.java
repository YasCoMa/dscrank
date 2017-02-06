package integrator.view;

import integrator.config.Meta_informations_integrator;

import java.io.File;
import java.nio.file.FileSystems;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Build_view extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Build_view(){
		super("yPublish - Strategy to help publish new data in Linked Open Data cloud");
		JTabbedPane pane = new JTabbedPane();  
		
	    Build_view_step1 obj_1 = new Build_view_step1 ();
	    pane.addTab( "DSCrawler", obj_1.join_all_sections() );
	    final Build_view_step2 obj_2 = new Build_view_step2();
	    pane.addTab( "Datasets Mapping", obj_2.join_all_sections() );
	    final Build_view_step3 obj_3 = new Build_view_step3();
	    pane.addTab( "Human Validation", obj_3.join_all_sections() );
	    final Build_view_step4 obj_4 = new Build_view_step4();
	    pane.addTab( "Pairs Fusion", obj_4.join_all_sections() ); // Complete
	    
	    Meta_informations_integrator mf = new Meta_informations_integrator();
	    pane.addTab("Help", mf.create_interface_help() );
	    pane.addTab("About", mf.create_interface_about() );
	    
	    ChangeListener changeListener = new ChangeListener() {
			public void stateChanged(ChangeEvent changeEvent) {
				JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
				int index = sourceTabbedPane.getSelectedIndex();
				if(index==1){
					obj_2.get_datasets(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_1");
				}
				if(index==2){
					// Verificar se existe configuração de crowdsourcing, se sim: -obter os dados do supervisor e do projeto; -Obter o crowd do pesquisador 
					Build_view_step3.n_pairs_.setText(obj_3.countPairs()+"");
					
					File a = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_3"+FileSystems.getDefault().getSeparator().toString()+"config_crowdsourcing.xml");
					if(a.exists()){
						obj_3.read_crowd_config("step_3");
						obj_3.enable_change_data_config();
						Build_view_step3.model_combo = new DefaultComboBoxModel<String>(obj_3.populate_combo_submissions());
						Build_view_step3.subs.setModel(Build_view_step3.model_combo);
					}
					
				}
				if(index==3){
					File a = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"step_3"+FileSystems.getDefault().getSeparator().toString()+"config_crowdsourcing.xml");
					if(a.exists()){
						obj_3.read_crowd_config("step_4");
						Build_view_step4.model_combo_subs = new DefaultComboBoxModel<String>(obj_4.populate_combo_submissions("loud"));
						Build_view_step4.submis.setModel(Build_view_step4.model_combo_subs);
					}
				}
			}
	    };
	    
	    pane.addChangeListener(changeListener);
	    
	    getContentPane().removeAll();
		getContentPane().add(pane);
	    
	}

	public static void main(String[] args)  {
		Build_view janela = new Build_view();
		janela.setSize(710,850);
	    janela.setLocationRelativeTo(null);
	    janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    janela.pack();
	    janela.setVisible(true);
    }
}
