package integrator.auto_mapping;

public class String_transformation {
	public String remove_identifier(String id, String label){
		if(id.contains("/")){
			id = id.split("/")[id.split("/").length-1];
		}
		String complete_id = " ["+id+"]";
		label=label.replace(complete_id, "");
		return label;
	}
	
	public static void main(String[] args){
		String_transformation st = new String_transformation();
		String id="drugbank_vocabulary:954363fbd0243552ddd09b24ad4c53d0";
		String g="Spectinomycine [drugbank_vocabulary:954363fbd0243552ddd09b24ad4c53d0]";
		System.out.println(st.remove_identifier(id, g));
	}
}
