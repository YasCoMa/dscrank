package integrator.dscrawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import integrator.view.Build_view_step1;

public class DSCrawler {
	private ArrayList<Seed> seeds = new ArrayList<Seed>();
	
	private Integer datasets = 0;
	
	private Set<Link> links = new HashSet<Link>();
    
	private Integer current_point = 0;
	private Integer maximum_level=0;
	
	private String navigation_file_path;
	
	private Boolean search_datasets=false;
	
	public void active_search_dataset(Boolean search_dataset){
		search_datasets = search_dataset;
	}
	
	public void setPathToNavigationFile(String path){
		navigation_file_path=path;
	}
	
	public String getPathToNavigationFile(){
		return navigation_file_path;
	}
	
	public void setMaxDepth(Integer level){
		maximum_level=level;
	}
	
	public Integer getMaxDepth(){
		return maximum_level;
	}
	
	public void cancelSeed(){
		seeds = new ArrayList<Seed>();
	}
	
	public void insertSeed(Seed seed){
		seeds.add(seed);
	}
	
	// Enviar para um banco de dados
	public void insertLink_to_process(Link link){
		links.add(link);
	    
	}
	public void removeLink_to_process(String url){
		links.remove(url);
	}
	
	public Set<Link> getLinks_to_process(){
		return links;
	}
	
	public ArrayList<Seed> getSeeds(){
		return seeds;
	}
	// Obter paginação boolean
	// Tópicos para encontrar padrões de estruturação da listagem dos datasets
	// Gravar num arquivo xml
	
	public void save_informations_datasets(String log_nav){
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder ;
		
		try {
			File f = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"temp_datasets"+FileSystems.getDefault().getSeparator().toString());
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("navigation");
			doc.appendChild(rootElement);
			int cont=1;
			for (File ob:f.listFiles()){
				
				try {
					@SuppressWarnings("resource")
					ObjectInputStream in = new ObjectInputStream(new FileInputStream(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"temp_datasets"+FileSystems.getDefault().getSeparator().toString()+ob.getName()));
		            Dataset d = (Dataset) in.readObject();
		            Element seed = doc.createElement("dataset");
					
					// set attribute to staff element
					Attr attr = doc.createAttribute("id");
					attr.setValue(cont+"");
					seed.setAttributeNode(attr);
					
					Element name_ = doc.createElement("name");
					name_.appendChild(doc.createTextNode(d.getName()));
					seed.appendChild(name_);
					
					Element url_ = doc.createElement("url");
					url_.appendChild(doc.createTextNode(d.getLink()));
					seed.appendChild(url_);
					
					Element description_ = doc.createElement("description");
					description_.appendChild(doc.createTextNode(d.getDescription()));
					seed.appendChild(description_);
					
					rootElement.appendChild(seed);
					
		        } 
		        catch (EOFException e) {
		            break;
		        } 
		        catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
		        catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				cont++;
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new File(getPathToNavigationFile()+FileSystems.getDefault().getSeparator().toString()+"dataset_information.xml"));
			transformer.transform(source, result);
			
			System.out.println("Datasets discovery file saved!");
			
			File file_ = new File(getPathToNavigationFile()+FileSystems.getDefault().getSeparator().toString()+"log_navigation.txt");
			FileWriter fw = new FileWriter(file_.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			if (!file_.exists()) {
				file_.createNewFile();
			}
			bw.write(log_nav);
			bw.close();
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
	
	@SuppressWarnings("unchecked")
	public void start_crawling(Document doc, Element rootElement, String log){
		//WorkerCrawler wc = new WorkerCrawler();
		//final Build_view_step1 b= new Build_view_step1();
    	
		try {
			//if(seeds.size()!=0){
				if (links.size()==0 && current_point==0){
					DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder docBuilder ;
					docBuilder = docFactory.newDocumentBuilder();
					doc = docBuilder.newDocument();
					rootElement = doc.createElement("navigation");
					doc.appendChild(rootElement);
					
					load_seeds();
					
					DatasetController dc = new DatasetController();
					dc.removeTempDatasets(); // Remove Dataset objects in the temporary folder
					
					System.out.println("Crawling activity started");
					log+="Crawling activity started";
					start_crawling(doc, rootElement, log);
				}
				else{
					Integer control=1;
					Set<Link> new_links = new HashSet<Link>();
					Set<Link> links_aux = links;
					if (links.size()!=0 && ((current_point<=getMaxDepth() && !search_datasets) || search_datasets) ){
						long time_now = System.currentTimeMillis();
						if(search_datasets){
							Iterator<Link> iterator = links_aux.iterator();
							while (iterator.hasNext()) {
								final Link l=iterator.next();
								System.out.println("Current URL: "+l.getUrl());
								log+="Current URL: "+l.getUrl()+"\n";
								
								if(l.getRoot().getStructure().equalsIgnoreCase("ckan")){
					            	ExtractionCKAN ec = new ExtractionCKAN();
					            	final ArrayList<Object> ds_data = ec.getDatasetData(l, current_point);
					            	datasets+= (Integer) ds_data.get(0);
					            	Set<Link> new_ = (Set<Link>) ds_data.get(1);
					            	new_links.addAll(new_);
					            	
					            	System.out.println("Datasets found:"+( (Integer) ds_data.get(0) ));
					            	log+="Datasets found:"+( (Integer) ds_data.get(0) )+"\n";
					            }
								
								if(l.getRoot().getStructure().equalsIgnoreCase("bio2rdf")){
					            	final ExtractionBio2RDF ec = new ExtractionBio2RDF();
					            	datasets += ec.getDatasetData(l);
					            	
					            	System.out.println("Datasets found:"+(ec.getDatasetData(l)));
									log+="Datasets found:"+(ec.getDatasetData(l))+"\n";
						        }
					            
					            String origem;
					            if(l.getLink_origem()==null){
									origem="root";
								}
					            else{
					            	origem = l.getLink_origem().getUrl();
					            }
					            rootElement.appendChild(create_element_link(""+current_point, l.getUrl(), origem, "yes", ""+control, doc));
					            control++;
					            
							}
						}
						else{
							// Verificar a negociação de conteúdo quando encontrar um arquivo
							Iterator<Link> iterator = links_aux.iterator();
							//System.out.println(links_aux.size());
							while (iterator.hasNext()) {
								Link l=iterator.next();
								String origem = l.getLink_origem().getUrl();
								if(l.getLink_origem()==null){
									origem="root";
								}
								
					            LinkController lc = new LinkController();
					            ArrayList<Link> lks = lc.getLinks(l);
					            Iterator<Link> iterator_link = lks.iterator();
								while (iterator_link.hasNext()) {
									Link lk=iterator_link.next();
									new_links.add(lk);
								}
								rootElement.appendChild(create_element_link(""+current_point, l.getUrl(), origem, "yes", ""+control, doc));
								control++;
							}
							
						}
						final long after = System.currentTimeMillis() - time_now;
						
						System.out.println("Running time of level "+current_point+": "+count_time(after));
						log+="Running time of level "+current_point+": "+count_time(after)+"\n";
								
						links=new_links;
						System.out.println("Number of links to be processed in next level: "+links.size());
						log+="Number of links to be processed in next level: "+links.size()+"\n";
						
						current_point++;
						
						if((links.size()==0)){
							TransformerFactory transformerFactory = TransformerFactory.newInstance();
							Transformer transformer = transformerFactory.newTransformer();
							DOMSource source = new DOMSource(doc);
							StreamResult result = new StreamResult(new File(getPathToNavigationFile()+FileSystems.getDefault().getSeparator().toString()+"links_mapping.xml"));
							transformer.transform(source, result);
							
							System.out.println("Link navigation file saved!");
							log+="Link navigation file saved!"+"\n";
							
							if(search_datasets && datasets!=0){
								save_informations_datasets(log);
							}
						}
						else{
							start_crawling(doc, rootElement, log);
						}
					}
					//System.out.println(current_point);
					
				}
			/*}
			else{
				wc.update_from_outside("You must insert seeds to start the crawling activity.");
			}*/
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
	}
	
	public void load_seeds(){
		File fXmlFile = new File(getPathToNavigationFile()+FileSystems.getDefault().getSeparator().toString()+"current_seeds.xml");
		DocumentBuilderFactory datasets = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = datasets.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("seed");
			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);
						
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					
					String url =  eElement.getElementsByTagName("url").item(0).getTextContent();
					String structure = eElement.getElementsByTagName("structure").item(0).getTextContent();
					String pages = eElement.getElementsByTagName("pagination").item(0).getTextContent();
					Boolean pagination = (pages.equalsIgnoreCase("Yes")) ? true : false;
					
					Seed l=new Seed();
					l.setUrl(url);
					l.setStructure(structure);
					l.setVerify_pagination(pagination);
					seeds.add(l);
				}
				ArrayList<Seed> seeds_aux = verify_number_pages(seeds);
				Iterator<Seed> iterator = seeds_aux.iterator();
				
				while (iterator.hasNext()) {
					Seed l=iterator.next();
					Link lk0 = new Link();
		            lk0.setLevel("0");
		            lk0.setUrl(l.getUrl());
		            lk0.setLink_origem(null);
		            lk0.setRoot(l);
		            lk0.setReturn_again(true);	
		            insertLink_to_process(lk0);
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
	}
	
	public String count_time(long t_total){
		String time="";
		long t_seconds = (long) (t_total/1000.0);
		if(t_seconds>60){
			long t_minutes = (int) (t_seconds/60);
			t_seconds = t_seconds%60;
			if(t_minutes<60){
				time+=t_minutes+" minute(s)"+( (t_seconds==0) ? "" : "; "+t_seconds+" second(s)" ); 
			}
			else{
				int t_hours = (int) (t_minutes/60);
				t_minutes = t_minutes%60;
				time+=t_hours+" hour(s)"+( (t_minutes==0) ? "" : "; "+t_minutes+" minute(s)" )+( (t_seconds==0) ? "" : "; "+t_seconds+" second(s)" ); 
			}
		}
		else {
			time+=t_seconds+" second(s)";
		}
		
		return time;
	}
	
	public Element create_element_link(String level, String url, String link_origin, String return_again, String control, Document doc){
		 // Seed elements
		Element seed = doc.createElement("link");
			
		// set attribute to staff element
		Attr attr = doc.createAttribute("id");
		attr.setValue(level+"-"+control);
		seed.setAttributeNode(attr);
		
		Element level_ = doc.createElement("level");
		level_.appendChild(doc.createTextNode(level));
		seed.appendChild(level_);
		
		Element url_ = doc.createElement("url");
		url_.appendChild(doc.createTextNode(url));
		seed.appendChild(url_);
		
		Element link_origin_ = doc.createElement("link_origin");
		link_origin_.appendChild(doc.createTextNode(link_origin));
		seed.appendChild(link_origin_);
		
		Element return_ = doc.createElement("return_again");
		return_.appendChild(doc.createTextNode(return_again));
		seed.appendChild(return_);
		
		return seed;
	}
	
	// Thesis (http://www.cs.rit.edu/~bmp4070/thesis.pdf) referencing text summarization with lucene (http://sujitpal.blogspot.com.br/2009/02/summarization-with-lucene.html)
	
	public Boolean verifyTopicsInLine(String structure_seed, String line){
		Boolean result=false;
		int cont=0;
		SeedController sc = new SeedController();
		for (String topic_in:sc.getTopics_in_line(structure_seed)){
			if(line.indexOf(topic_in)!=-1){
				cont++;
			}
		}
		if(cont>0){
			result=true;
		}
		return result;
	}
	
	public Boolean verifyTopicsOutLine(String structure_seed, String line){
		Boolean result=false;
		int cont=0;
		SeedController sc = new SeedController();
		for (String topic_out:sc.getTopics_out_line(structure_seed)){
			if(line.indexOf(topic_out)==-1){
				cont++;
			}
		}
		if(cont==sc.getTopics_out_line(structure_seed).size()){
			result=true;
		}
		return result;
	}
	
	public ArrayList<Seed> verify_number_pages(ArrayList<Seed> seeds2){
		String stop="»";
		URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        ArrayList<Seed> seeds_aux = new ArrayList<Seed>();
        Iterator<Seed> iterator_aux = seeds2.iterator();
		while (iterator_aux.hasNext()) {
			Seed l=iterator_aux.next();
			
			if(l.getVerify_pagination()){
		        try {
		            //List<NameValuePair> params = new LinkedList<NameValuePair>();
		            //params.add(new BasicNameValuePair("tags", "format-rdf"));
		            //String paramString = URLEncodedUtils.format(params, "utf-8");
		            url = new URL(l.getUrl());
		           
		            conn = (HttpURLConnection) url.openConnection();
		            conn.setRequestMethod("GET");
		            
		            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		            Integer pag_max=0;
		            while ((line = rd.readLine()) != null) {
		            	if (line.indexOf("page=")!=-1 ){
		            		String[] partes = line.split("</li>");
		            		for (int j=0; j<partes.length; j++){
		            			if (partes[j].indexOf("page=")!=-1 && partes[j].indexOf(stop)==-1){
			            			partes[j]=partes[j].substring(partes[j].indexOf("href")+6);
			            			//System.out.println(partes[j]);
			            			pag_max = Integer.parseInt( partes[j].substring(partes[j].indexOf("page=")+5, partes[j].indexOf("\"")) );
			            		}
		            		}
		            	}
		            }
		        	//System.out.println(pag_max);
	    			
		            if(pag_max!=0 && pag_max!=1){
		            	for (int i=1; i<=pag_max; i++){
		            		Seed se = new Seed();
	            			se.setStructure(l.getStructure());
	            			se.setVerify_pagination(l.getVerify_pagination());
		            		if(l.getUrl().indexOf("?")!=-1){
		            			se.setUrl(l.getUrl()+"&page="+i);
		            		}
		            		else{
		            			se.setUrl(l.getUrl()+"?page="+i);
		            		}
		            		seeds_aux.add(se);
		                }
		            }
		            else{
		            	seeds_aux.add(l);
		            }
		            rd.close();
		        }
		        catch(FileNotFoundException ef){
		        	System.out.println("erro[Url não encontrada!]");
		        }
		        catch (Exception e) {
		            e.printStackTrace();
		        }
			}
			else{
				seeds_aux.add(l);
			}
		}
		return seeds_aux;
	}
	
	public static void main (String[] args){
		DSCrawler cr = new DSCrawler();
		Seed a = new Seed();
		a.setUrl("http://linkeddatacatalog.dws.informatik.uni-mannheim.de/dataset?sort=score+desc%2C+metadata_modified+desc&q=&tags=lifesciences&_tags_limit=0");
		a.setVerify_pagination(true);
		a.setStructure("ckan");
		//cr.insertSeed(a);
		
		Seed b = new Seed();
		b.setUrl("http://download.bio2rdf.org/release/3/release.html");
		b.setVerify_pagination(false);
		b.setStructure("bio2rdf");
		cr.insertSeed(b);
		
		cr.setMaxDepth(1);
		cr.setPathToNavigationFile("text_example");
		cr.active_search_dataset(true);
		cr.start_crawling(null, null,"");
	}
}

