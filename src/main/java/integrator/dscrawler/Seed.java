package integrator.dscrawler;

public class Seed {
	private String url;
	private Boolean verify_pagination = false;
	private String structure;
	
	public String getStructure() {
		return structure;
	}

	public void setStructure(String structure) {
		this.structure = structure;
	}

	// If seed is for dataset discovery, configure topics or key search to help the html page filtering
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Boolean getVerify_pagination() {
		return verify_pagination;
	}

	public void setVerify_pagination(Boolean verify_pagination) {
		this.verify_pagination = verify_pagination;
	}

	
}
