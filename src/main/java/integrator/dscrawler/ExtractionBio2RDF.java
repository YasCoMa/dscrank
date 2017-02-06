package integrator.dscrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;

public class ExtractionBio2RDF {
	
	public Integer getDatasetData(Link lk){
		Integer count_datasets = 0;
		
		Boolean init_description=false;
		HttpURLConnection conn;
        BufferedReader rd;
		try {
			URL url = new URL(lk.getUrl());
			conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line, name="", description="";
            
            while ((line = rd.readLine()) != null) {	
				
	        	if (line.indexOf("<strong")!=-1 && line.indexOf("href")!=-1 ){
	        		name= line;
	        		name=name.replace(name.substring(name.indexOf("<strong"), name.indexOf(">")+1), "");
	        		name=name.replace("</strong>", "");
	        		Pattern p = Pattern.compile(">(.*?)</a>");
	    			Matcher m = p.matcher(name);
	    			if (m.find()) {
	    			    name = m.group(1); // this variable should contain the link URL
	    			}
	        		name = Jsoup.parse(name).text();
	        		//System.out.println(name);
	        		
	        		init_description=true;
	        		description="";
	        		line=rd.readLine();
	        		while (init_description) {
	        			description+=line;
	        			
	        			line = rd.readLine();
	        			if(line.indexOf("<strong>")!=-1){
	        				init_description=false;
	        			}
	        		}
	        		description = Jsoup.parse(description).text();
	        		
	        		while(!line.contains("/sparql\"")){
	        			line = rd.readLine();
	        		}
	        		String link=line;
            		link=link.substring(link.indexOf("href")+6, link.indexOf("\">"));
            		Link lk0 = new Link();
		            lk0.setLevel((Integer.parseInt(lk.getLevel())+1)+"");
		            lk0.setUrl(link);
		            lk0.setLink_origem(lk);
		            lk0.setRoot(lk.getRoot());
		            lk0.setReturn_again(true);
            		
            		create_ds(name, lk0, description);
            		count_datasets++;
	        		
	         	}
	        	
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
		return count_datasets;
	}
	
	public void create_ds(String name, Link l, String description){
		Dataset d = new Dataset ();
		d.setName(name);
		d.setLink(l.getUrl());
		d.setDescription(description);
		
		DatasetController dc = new DatasetController();
		dc.serialize(d);
		
	}
}
