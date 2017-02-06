package integrator.dscrawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileSystems;
import java.text.Normalizer;

import integrator.view.Build_view_step1;

public class DatasetController {
	
	public void removeTempDatasets() {
		File f = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"temp_datasets"+FileSystems.getDefault().getSeparator().toString());
        if (f.isDirectory()) {
            /* Lista todos os arquivos do diretório em um array
               de objetos File */
            File[] files = f.listFiles();
            // Identa a lista (foreach) e deleta um por um
            for (File file : files) {
                    file.delete();
            }
        }
	}
	
	public static String modifyObjectName(String acentuada) {
		String modified="";
	    CharSequence cs = new StringBuilder(acentuada);  
	    modified= Normalizer.normalize(cs, Normalizer.Form.NFKD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	    modified = modified.toLowerCase();
	    modified = modified.replace(" ", "_");
	    modified = modified.replace("-", "_");
	    modified = modified.replace(":", "");
	    modified = modified.replace(",", "");
	    modified = modified.replace(";", "");
	    modified = modified.replace(".", "");
	    return modified;
	}  
	
	public void serialize(Dataset obj) {
        FileOutputStream outFile;
		try {
			File g = new File(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"temp_datasets");
			if(!g.exists()){
				g.mkdir();
			}
			outFile = new FileOutputStream(Build_view_step1.application_dir+FileSystems.getDefault().getSeparator().toString()+"workspace_ypublish"+FileSystems.getDefault().getSeparator().toString()+"temp_datasets"+FileSystems.getDefault().getSeparator().toString()+(modifyObjectName(obj.getName()))+".ser");
			ObjectOutputStream s = new ObjectOutputStream(outFile);
	        s.writeObject(obj);
	        s.close();
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Invalid path");
		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//System.out.println("Error in writing operation");
		}
        
	}
	
	public Dataset deserializar(String path) throws Exception {
	    FileInputStream inFile = new FileInputStream(path);
	    ObjectInputStream d = new ObjectInputStream(inFile);
	    Dataset o = (Dataset) d.readObject();
	    d.close();
	    return o;
	}
	
	
}
