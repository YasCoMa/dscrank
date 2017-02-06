package integrator.dscrawler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SeedController {
	private HashMap<String, Set<String>> add_topics = new HashMap<String, Set<String>>();
	private HashMap<String, Set<String>> remove_topics = new HashMap<String, Set<String>>();
	
	public void insertTopicInLine(String structure, String topic){
		if(add_topics.containsKey(structure)){
			Set<String> atual = add_topics.get(structure);
			atual.add(topic);
			add_topics.replace(structure, atual);
		}
		else{
			Set<String> atual = new HashSet<String>();
			atual.add(topic);
			add_topics.put(structure, atual);
		}
	}
	
	public void removeTopicInLine(String structure, String topic){
		if(add_topics.containsKey(structure)){
			Set<String> atual = add_topics.get(structure);
			if(atual.contains(topic))
				atual.remove(topic);
			add_topics.replace(structure, atual);
		}
	}
	
	public void insertTopicOutLine(String structure, String topic){
		if(remove_topics.containsKey(structure)){
			Set<String> atual = remove_topics.get(structure);
			atual.add(topic);
			remove_topics.replace(structure, atual);
		}
		else{
			Set<String> atual = new HashSet<String>();
			atual.add(topic);
			remove_topics.put(structure, atual);
		}
	}
	
	public void removeTopicOutLine(String structure, String topic){
		if(remove_topics.containsKey(structure)){
			Set<String> atual = remove_topics.get(structure);
			if(atual.contains(topic))
				atual.remove(topic);
			remove_topics.replace(structure, atual);
		}
	}

	public Set<String> getTopics_in_line(String structure) {
		return add_topics.get(structure);
	}

	public Set<String> getTopics_out_line(String structure) {
		return remove_topics.get(structure);
	}
}
