package integrator.dscrawler.simProperty;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingFactory;
import org.apache.jena.sparql.engine.iterator.QueryIterSingleton;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.pfunction.PropFuncArg;
import org.apache.jena.sparql.pfunction.PropertyFunctionBase;

import integrator.dscrawler.TextPreProcessingRanking;
import uk.ac.shef.wit.simmetrics.similaritymetrics.JaroWinkler;

public class transform_string extends PropertyFunctionBase {
	  
	@Deprecated
  public PropFuncArg evalIfExists(PropFuncArg prop, Binding binding){
      return Substitute.substitute(prop, binding) ;
  }
	
	@Override
	public QueryIterator exec(Binding binding, PropFuncArg subject,
		Node predicate, PropFuncArg object, ExecutionContext execCxt) {

	    subject = evalIfExists(subject, binding);
	    Node s = subject.getArg();
	
	    object = evalIfExists(object, binding);
	    Node arg1 = object.getArg(0);
	
	    String a = null;
	    if (arg1.isLiteral() ) {
	      a = arg1.getLiteralLexicalForm();
	    } 
	    else if (arg1.isURI()) {
	      a = arg1.getLocalName();
	    } 
	    else {
	      System.out.println("Error: Node types unequal.");
	      System.out.println(arg1.toString());
	    }
	
	    TextPreProcessingRanking t = new TextPreProcessingRanking();
	    //a=a.replace("_", " ");
	    String q_a="";
	    if(a.indexOf(" ")!=-1){
	    	String[] args=a.split(" ");
	    	for(int i=0;i<args.length;i++){
	    		if(i==0){
	    			args[i]=t.remove_punctuation(args[i]);
	    		    args[i]=t.getStemmedTokens(args[i]);
	    		    q_a=""+args[i]+"";
	    		    
	    		}
	    		/*else{
	    			if(i==1){
	    				q_a+="(";
	    			}
	    			q_a+=args[i];
	    			if(i==args.length-1){
	    				q_a+=")";
	    			}
	    			else{
	    				q_a+=" | ";
	    			}
	    		}*/
	    	}
	    }
	    else{
	    	q_a=t.remove_punctuation(a);
	    	q_a=t.getStemmedTokens(q_a);
	    }
	
	    NodeValue nv = NodeValue.makeString(q_a);
	    Binding bind = BindingFactory.binding(binding, Var.alloc(s), nv.asNode());
	    QueryIterSingleton q = QueryIterSingleton.create(bind, execCxt);
	    return q;
	}
}
