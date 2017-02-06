package integrator.config;

import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

public class Meta_informations_integrator {
	
	public JPanel create_interface_help(){
		JPanel help = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		JTextArea aviso_help = new JTextArea(20,80);
		
		String help_english = "    This is a system for data interlinking following the Linked Data's best practices."
				+ "\n    It works with the following steps:"
				+ "\n    1 - Dataset Selection and Ranking.\n"
				+ "\n    2 - Mapping between the source and the selected target datasets\n"
				+ "\n    3 - Submission of the pairs to generate the tasks for crowd validation.\n"
				+ "\n    4 - Fusion between the source dataset resources with their candidate pairs according to the major classification voted.\n"
				;
		
		aviso_help.setText(help_english);
		aviso_help.setEditable(false);
		JScrollPane scroll = new JScrollPane(aviso_help, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		help.add(scroll);
		
		return help;
	}
	
	public JPanel create_interface_about(){
		JPanel about = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		JTextArea aviso_about = new JTextArea(20,65);
		
		String about_english = "	This system is an implementation of yPublish' strategy, which was created to help researchers publishing data provided by their studies and compiled resulting data following the best practices of Linked Data Project. The yPublish's main goal is helping the source selection, mapping and interlinking processes.\n"
				+ "\n    Author and developer of the system's core and graphic interface: Yasmmin Côrtes Martins";
		
		aviso_about.setText(about_english);
		aviso_about.setEditable(false);
		JScrollPane scroll = new JScrollPane(aviso_about, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		about.add(scroll);
		
		return about;
	}
}
