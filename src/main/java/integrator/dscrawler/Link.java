package integrator.dscrawler;

public class Link {
	private String url;
	private String level;
	private Boolean return_again;
	private Link link_origem;
	private String label;
	private Seed root; 
	private String contentType;
	
	public Seed getRoot() {
		return root;
	}
	public void setRoot(Seed root) {
		this.root = root;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public Link getLink_origem() {
		return link_origem;
	}
	public void setLink_origem(Link link_origem) {
		this.link_origem = link_origem;
	}
	public Boolean getReturn_again() {
		return return_again;
	}
	public void setReturn_again(Boolean return_again) {
		this.return_again = return_again;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}
	
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String type) {
		this.contentType = type;
	}
}
