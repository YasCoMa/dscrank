package integrator.dscrawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;

public class ExtractionCKAN {
	
	public ArrayList<Object> getDatasetData(Link lk, Integer current){
		ArrayList<Object> ds=new ArrayList<Object>();
		
		Integer count_datasets =0;
		Set<Link> new_links = new HashSet<Link>();
		
		LinkController lc = new LinkController();
		Boolean init_description=false;
		
		HttpURLConnection conn;
        BufferedReader rd;
		try {
			URL url = new URL(lk.getUrl());
			conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            
        	if(current==0){
        		while ((line = rd.readLine()) != null) {
		        	if ( (line.indexOf("/dataset/")!=-1 && line.indexOf("data-format")==-1)){
		        		//String link = line.substring(line.indexOf("href=\"")+6, line.indexOf("\""));
		        		Dataset d = new Dataset();
		        		
		        		String nome=line;
		        		nome=nome.replace(nome.substring(nome.indexOf("<a "), nome.indexOf(">")+1), "");
		        		nome=nome.replace("</a>", "");
		        		nome = Jsoup.parse(nome).text();
		        		d.setName(nome);
		        		
		        		String link=line;
		        		link=link.substring(link.indexOf("href")+6, link.indexOf("\">"));
		        		if(link.startsWith("/")){
		        			String rot = lk.getUrl().split("\\?")[0];
		        			link=rot+link.replace("/dataset", "");
		        		}
		        		Link lk0 = new Link();
			            lk0.setLevel((Integer.parseInt(lk.getLevel())+1)+"");
			            lk0.setUrl(link);
			            lk0.setLink_origem(lk);
			            lk0.setRoot(lk.getRoot());
			            lk0.setReturn_again(true);
		        		d.setLink(lk0.getUrl());
		        		
		        		//System.out.println(link);
		        		Boolean achou_div=false;
		        		line = rd.readLine();
		        		
		        		String description ="";
		        		while (!achou_div) {
		        			if(line.indexOf("<div")!=-1 && line.indexOf("pagination")==-1){
		            			achou_div=true;
		            			description=line.replace("<div>", "");
		            			init_description=true;
		        			}
		        			line = rd.readLine();
		        		}
		        		while (init_description) {
		        			if(line.indexOf("</div>")!=-1){
		        				description+=line;
		        			}
		        			line = rd.readLine();
		        			if(line.indexOf("</div>")!=-1){
		        				init_description=false;
		        			}
		        		}
		        		description = Jsoup.parse(description).text();
		        		d.setDescription(description);
		        		System.out.println(description);
		        		
		        		if(!description.equalsIgnoreCase("")){
			        		if(!d.getName().equalsIgnoreCase("none")){
			            		if(!d.getLink().contains("sparql")){
			            			ArrayList<String> urls_rdf = lc.find_rdf_in_links(lk0, 0);
			            			
			            			if(urls_rdf.size()>0){
			            				String url_source=getLinkSource(lk0.getUrl());
				            			if(!url_source.equalsIgnoreCase("") && url_source.contains("www4.wiwiss.fu-berlin.de")){
				            				ArrayList<String> expanded_urls_rdf = lc.find_rdf_in_links(url_source);
				            				urls_rdf.addAll(expanded_urls_rdf);
			            				}
				            			
				            			String uris="";
		            					
		            					for(String uri:urls_rdf){
		            						uris+=uri+"@";
		            					}
		            					d.setDescription(getFullDescriptionDataset(link));
		            					lk0.setUrl(uris);
		            					d.setLink(lk0.getUrl());
		            					
		            					DatasetController dc = new DatasetController();
			            				dc.serialize(d);
			            				
			            				count_datasets++;
			            			}
			            			else{
			            				if(lc.isSameDomain(lk.getUrl(), lk0.getUrl())){
			            					new_links.add(lk0);
			            				}
				            		}
			            		}
		            		}
		        		}
		        	}
	        	}
        	}
        	if(current==1){
        		if(lc.hasSource(lk)){
        			while ((line = rd.readLine()) != null) {
		        		if ( line.indexOf("<article")!=-1 ){
		        			Dataset d = new Dataset();
		        			
		        			while(line.indexOf("<h1")==-1){
		        				line = rd.readLine();
		        			}
		        			line = rd.readLine();
		        			String name=line;
		        			
		        			while(line.indexOf("</h1")==-1){
		        				line = rd.readLine();
		        				name+=line;
		        			}
		        			name=name.replace("</h1>", "");
		        			name = Jsoup.parse(name).text();
		        			d.setName(name);
		        			
		        			String description="";
		        			while(line.indexOf("notes embedded-content")==-1){
		        				line = rd.readLine();
		        				if(line==null){
		        					break;
		        				}
		        			}
		        			if (line!=null){
			        			line = rd.readLine();
			        			description=line;
			        			while(line.indexOf("</div")==-1){
			        				line = rd.readLine();
			        				description+=line;
			        			}
			        			description=description.replace("<p>", "");
			        			description=description.replace("</p>", " ");
			        			description=description.replace("</div>", "");
			        			description = Jsoup.parse(description).text();
			        			
			        			d.setDescription(description);
			        			
			        			while(line.indexOf("dataset-label")==-1 || line.indexOf("Source")==-1){
			                		//System.out.println(line);
			        				line = rd.readLine();
			        				if(line==null){
			        					break;
			        				}
			        			}
			        			line=rd.readLine();
			        			if(line!=null){
			    	    			line = rd.readLine();
			    	    			Pattern p = Pattern.compile("href=\"(.*?)\"");
				        			Matcher m = p.matcher(line);
				        			String link = "";
				        			if (m.find()) {
				        			    link = m.group(1); // this variable should contain the link URL
				        			}
				        			
				        			Link lk0 = new Link();
						            lk0.setLevel((Integer.parseInt(lk.getLevel())+1)+"");
						            lk0.setUrl(link);
						            lk0.setLink_origem(lk);
						            lk0.setRoot(lk.getRoot());
						            lk0.setReturn_again(true);
				        			d.setLink(lk0.getUrl());
				        			
				        			if(!d.getName().equalsIgnoreCase("none")){
					            		if(!d.getLink().contains("/sparql")){
					            			ArrayList<String> urls_rdf = lc.find_rdf_in_links(lk0, 1);
				            				if(urls_rdf.size()>0){
				            					String uris="";
				            					for(String uri:urls_rdf){
				            						uris+=uri+"@";
				            					}
				            					lk0.setUrl(uris);
				            					d.setLink(lk0.getUrl());
				            					
				            					DatasetController dc = new DatasetController();
					            				dc.serialize(d);
					            				
					            				count_datasets++;
					            			}
					            			else{
					            				if(lc.isSameDomain(lk.getUrl(), lk0.getUrl())){
					            					new_links.add(lk0);
					            				}
						            		}
					            		}
				            		}
			                	}
		        			}
		        			System.out.println(description);
		        		}
        			}
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
		ds.add(count_datasets);
    	ds.add(new_links);
    	
		return ds;
	}
	public String getLinkSource(String url_){
		String uri_source=""; 
		
		HttpURLConnection conn;
        BufferedReader rd;
		try {
			URL url = new URL(url_);
			conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            
            while ((line = rd.readLine()) != null) {
            	while(line.indexOf("dataset-label")==-1 || line.indexOf("Source")==-1){
            		
    				line = rd.readLine();
    				if(line==null){
    					break;
    				}
    			}
            	if(line!=null){
	    			line = rd.readLine();
	    			Pattern p = Pattern.compile("href=\"(.*?)\"");
	    			Matcher m = p.matcher(line);
	    			
	    			if (m.find()) {
	    			    uri_source = m.group(1); // this variable should contain the link URL
	    			}
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
		return uri_source;
	}
	public String getFullDescriptionDataset(String url){
		String description="";
		
		Link lk = new Link();
		lk.setUrl(url);
		LinkController lc = new LinkController();
		ArrayList<String> lines = lc.getHTML(lk);
		Iterator<String> iter_line = lines.iterator();
		if(iter_line.hasNext()){
        	String line = iter_line.next();
			while(line.indexOf("notes embedded-content")==-1){
				if(iter_line.hasNext()){
					line = iter_line.next();
				}
			}
			if(iter_line.hasNext()){
				line = iter_line.next();
				description=line;
				while(line.indexOf("</div")==-1){
					line = iter_line.next();
					description+=line;
				}
				description=description.replace("<p>", "");
				description=description.replace("</p>", " ");
				description=description.replace("</div>", "");
				description = Jsoup.parse(description).text();
			}
		}
		
		return description;
	}
	
	public static void main(String[] args){
		ExtractionCKAN a = new ExtractionCKAN();
		ArrayList<Object> i = new ArrayList<Object>();
		Link f = new Link();
		f.setLevel("0");
		f.setUrl("https://datahub.io/dataset/allen-brain-atlas");
		i=a.getDatasetData(f, 1);
		System.out.println(i.get(0));
	}
}
