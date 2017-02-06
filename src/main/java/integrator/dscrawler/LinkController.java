package integrator.dscrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.FileNameMap;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RiotException;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openrdf.model.Statement;
import org.openrdf.model.vocabulary.RDFS;

public class LinkController {
	
	public String getTitle(Link lk){
		String title="";
		String HTMLPage="";
		for (String line:getHTML(lk)){
			HTMLPage+=line+"\n";
		}
		Pattern linkPattern = Pattern.compile("<title(.*?)>(.*?)</title>",  Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
		Matcher pageMatcher = linkPattern.matcher(HTMLPage);
		if(pageMatcher.find()){
			title=pageMatcher.group();
		}
		
		if (!title.equalsIgnoreCase("")){
			title=title.replace(title.substring(title.indexOf("<title"), title.indexOf(">")+1), "");
			title=HtmlManipulator.replaceHtmlEntities(title.replace("</title>", ""));
		}
		return title;
	}
	
	public String getText(Link lk){
		String TextPage="";
		String HTMLPage="";
		for (String line:getHTML(lk)){
			HTMLPage+=line+"\n";
		}
		TextPage=Jsoup.parse(HTMLPage).text();
		return TextPage;
	}
	
	public String get_redirected_url(String url){
		String right_url=url;
		HttpURLConnection con;
		try {
			con = (HttpURLConnection) new URL( url ).openConnection();
			con.setReadTimeout(30000);
			con.setInstanceFollowRedirects( false );
			con.connect();
			String location = con.getHeaderField( "Location" );
			right_url=location;
		
		} 
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			
		}
		catch (java.net.UnknownHostException e){
			
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
		}
		return right_url;
	}
	
	public Boolean hasSource(Link lk){
		System.out.println("init-source");
		Boolean source=false;
		for (String line:getHTML(lk)){
			if(line.indexOf("dataset-label")!=-1 && line.indexOf("Source")!=-1){
				source=true;
				break;
			}
		}
		System.out.println("source");
		return source;
	}
	
	public ArrayList<String> getHTML(Link lk){
		ArrayList<String> lines_html =  new ArrayList<String>();
		HttpURLConnection conn;
        BufferedReader rd;
		try {
			URL url = new URL(lk.getUrl());
			conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
            	lines_html.add(line);
            }
		} 
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			System.out.print("URL is not valid.");
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("HTTP response code was not 200 [ok]");
		}
		
		return lines_html;
	}
	
	public Boolean filter_image(String url){
		Boolean flag = false;
		Pattern IMAGE_EXTENSIONS = Pattern.compile(".*\\.(bmp|gif|jpg|png)$");
		if(IMAGE_EXTENSIONS.matcher(url.toLowerCase()).matches()){
			flag = true;
		}
		return flag;
	}
	
	public Boolean filter_file(String url){
		Boolean flag = false;
		Pattern FILE_EXTENSIONS = Pattern.compile(".*\\.(pdf|zip|rar|doc|xls|docx|xlsx)$");
		if(FILE_EXTENSIONS.matcher(url.toLowerCase()).matches()){
			flag = true;
		}
		return flag;
	}
	
	public Boolean url_existing(ArrayList<Link> urls, String new_url){
		Boolean a=false;
		for(Link lk:urls){
			if(lk.getUrl().equalsIgnoreCase(new_url)){
				a=true;
				break;
			}
		}
		return a;
	}
	
	// Get links from the area related to the dataset only
	public ArrayList<Link> getLinks(Link lk){
		ArrayList<Link> next_links = new ArrayList<Link>();
		
		Document doc;
		try {
			try{
				LinkController lb = new LinkController();
				String lki= (lb.get_redirected_url(lk.getUrl())!=null) ? lb.get_redirected_url(lk.getUrl()) : lk.getUrl();
				doc = Jsoup.connect(lki).get();
				//Elements links = doc.select("a[href]");
		        Elements links = doc.select("section#dataset-resources").select("a[href]");
		        
		        for (Element link : links) {
		            //System.out.println(" * a: <"+link.attr[("abs:href")+"-"+link.text()+"> ");
		            
		        	if(!url_existing(next_links, link.attr("abs:href"))){
			            Link aux = new Link();
			            if(lk.getLevel()!=null){
			            	Integer next_level=(Integer.parseInt(lk.getLevel())+1);
							aux.setLevel( next_level.toString() );
			            }
						aux.setReturn_again(true);
						aux.setUrl(link.attr("abs:href"));
						aux.setLabel(link.text());
						aux.setLink_origem(lk);
						
						aux.setContentType(lb.getContentType(aux.getUrl()));
					    next_links.add(aux);
		        	}
					
				}
			}
			catch(HttpStatusException e1){
				System.out.println("Invalid HTTP status response.");
			}    
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed in fetch the URL.");
		}
        catch (IllegalArgumentException e1){
        	System.out.println("Invalid URL found.");
        }
		
		return next_links;
	}
	
	// get all the links of some page 
	public ArrayList<Link> getLinksGeneric(Link lk){
		ArrayList<Link> next_links = new ArrayList<Link>();
		
		Document doc;
		try {
			try{
				LinkController lb = new LinkController();
				String lki= (lb.get_redirected_url(lk.getUrl())!=null) ? lb.get_redirected_url(lk.getUrl()) : lk.getUrl();
				doc = Jsoup.connect(lki).get();
				Elements links = doc.select("a[href]");
		        // Testar Elements links = doc.select("section#dataset-resources").select("a[href]");
		        for (Element link : links) {
		            //System.out.println(" * a: <"+link.attr("abs:href")+"-"+link.text()+"> ");
		            
		            Link aux = new Link();
		            if(lk.getLevel()!=null){
		            	Integer next_level=(Integer.parseInt(lk.getLevel())+1);
						aux.setLevel( next_level.toString() );
		            }
					aux.setReturn_again(true);
					aux.setUrl(link.attr("abs:href"));
					aux.setLabel(link.text());
					aux.setLink_origem(lk);
					
					aux.setContentType(lb.getContentType(aux.getUrl()));
					next_links.add(aux); 
		        }
			}
			catch(HttpStatusException e1){
				System.out.println("Invalid HTTP status response.");
			}    
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Failed in fetch the URL.");
		}
        catch (IllegalArgumentException e1){
        	System.out.println("Invalid URL found.");
        }
		
		return next_links;
	}
	public String getContentType(String url){
		String type="";
		try{
			
			URL uri = new URL(url);
			HttpURLConnection http = (HttpURLConnection)uri.openConnection();
			http.setConnectTimeout(5 * 1000);
			http.setReadTimeout(5000);
			if(http.getResponseCode()==200){
			    type = http.getContentType();
				type = type.split(";")[0];
			}
			else{
				type="none";
			}
			
			/*
			DefaultHttpClient httpclient = new DefaultHttpClient();
			HttpResponse response = httpclient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			content_type = entity.getContentType().getValue();
			if (entity != null){
				System.out.println(content_type);
			}
			*/
		}
		catch (ClientProtocolException e) {
			
		}
		catch (IOException e) {
			
		}
		catch(NullPointerException e){
			type="none";
		}
		catch(ClassCastException e){
			type="none";
		}
		
		return type;
	}
	
	public Boolean filter_rdf_content(Link l){
		Boolean res = false;
		String content_type=l.getContentType();
		if(content_type.contains("application/rdf+xml") || content_type.contains("application/x-turtle") || content_type.contains("text/rdf+n3") || content_type.contains("text/n3") || content_type.contains("text/turtle")){
			res=true;
		}
		if(content_type.contains("text/plain") && !l.getUrl().endsWith(".nq") && verify_conversion_plainToRdfxml(l.getUrl())){
			res=true;
		}
		return res;
	}
	
	public Boolean verify_conversion_plainToRdfxml(String l){
		Boolean res=false;
		try{
			int cont=0;

			HttpURLConnection conn;
	        BufferedReader rd;
			try {
				URL url = new URL(l);
				conn = (HttpURLConnection) url.openConnection();
	            conn.setRequestMethod("GET");
	            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	            String s;
	            
	            while ((s = rd.readLine()) != null) {
					if(cont==50 || s.toLowerCase().contains("prefix") || s.toLowerCase().contains("<rdf:rdf") || s.toLowerCase().contains("<http://www.w3.org/2000/01/rdf-schema")){
						res=true;
						break;
					}
					cont++;
	            }
			}
			catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				System.out.print("URL is not valid.");
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("HTTP response code was not 200 [ok]");
			}
			//Model model = ModelFactory.createDefaultModel();
	        //model.read(url);
	        //res=true;
        }
        catch(RiotException  re){
        	System.out.println("texto plano");
        }
		return res;
	}
	
	public Boolean isSameDomain(String root, String other_url){
		Boolean sd = false;
		URL url, other;
		try {
			url = new URL(root);
			other = new URL(other_url);
			
			if(other.getHost().startsWith(url.getHost())){
				sd=true;
			}
		} 
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			System.out.println("Invalid URL address");
		}
		
		return sd;
	}
	
	public ArrayList<String> find_rdf_in_links(Link l, Integer level){
		ArrayList<String> urls_rdf = new ArrayList<String>();
		
		LinkController lc = new LinkController();
		ArrayList<Link> k=null;
		if(level==0){
			k =lc.getLinks(l);
		}
		else{
			k =lc.getLinksGeneric(l);
		}
		System.out.println(l.getUrl()+" - "+k.size());
		
		for (Link lk:k){
			
			if(!lc.isSameDomain(lk.getUrl(),l.getUrl())){
				if(!lc.filter_image(l.getUrl()) && !lc.filter_file(l.getUrl())){
					if(lc.filter_rdf_content(lk) || lk.getUrl().contains("sparql")){
						/*if(lk.getUrl().contains("eagle-i")){
							lk.setUrl(lk.getUrl().replace("sparqler/sparql", "sparqler/query"));
						}*/
						urls_rdf.add(lk.getUrl());
					}
				}
			}
		}
		System.out.println("finish");
		return urls_rdf;
	}
	
	// Variation of the method showed above to expand the link discovery for the link given as source by the dataset information's provider in the catalog
	public ArrayList<String> find_rdf_in_links(String url){
		ArrayList<String> urls_rdf = new ArrayList<String>();
		
		LinkController lc = new LinkController();
		Link l = new Link();
		l.setUrl(url);
		for (Link lk:lc.getLinksGeneric(l)){
			if(!lc.filter_image(l.getUrl()) && !lc.filter_file(l.getUrl())){
				if(lc.filter_rdf_content(lk)){
					if(lk.getUrl().indexOf("/all")!=-1){
						try{
							Model m = ModelFactory.createDefaultModel();
							m.read(lk.getUrl());
							StmtIterator i = m.listStatements();
							while(i.hasNext()){
								org.apache.jena.rdf.model.Statement s = i.next();
								if(s.getPredicate().toString().equalsIgnoreCase(RDFS.SEEALSO.toString())){
									if(!urls_rdf.contains(s.getObject().toString())){
										urls_rdf.add(s.getObject().toString());
									}
								}
							}
						}
						catch(RiotException e){
							
						}
					}
				}
			}
		}
		
		return urls_rdf;
	}
	
	public Boolean verify_url(String url_seed){
		try {
			URL url = new URL(url_seed);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            @SuppressWarnings("unused")
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            return true;
		}
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			return false;
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
		}
	}
	
	public static void main(String[] args){
		
		LinkController l = new LinkController();
		//System.out.println(l.filter_rdf_content("http://wifo5-04.informatik.uni-mannheim.de/drugbank/all"));
		//System.out.println(l.verify_url("http://asdbiuasf.com"));
		ArrayList<String> a = l.find_rdf_in_links("http://www4.wiwiss.fu-berlin.de/dailymed/");
		for (String d:a){
			System.out.println(d);
		}
		
		System.out.println(l.getContentType("bio2rdf.org/drugbank:DB00001"));
		
	}
}
