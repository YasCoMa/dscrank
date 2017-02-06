package integrator.auto_mapping.isparqlProperties;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Substitute;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.*;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.iterator.QueryIterSingleton;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.pfunction.PropFuncArg;
import org.apache.jena.sparql.pfunction.PropertyFunctionBase;

import info.debatty.java.stringsimilarity.*;

public class metric_lcs extends PropertyFunctionBase {
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
	    Node arg2 = object.getArg(1);
	
	    String a = null, b = null;
	    if (arg1.isLiteral() && arg2.isLiteral()) {
	      a = arg1.getLiteralLexicalForm();
	      b = arg2.getLiteralLexicalForm();
	    } 
	    else if (arg1.isURI() && arg2.isURI()) {
	      a = arg1.getLocalName();
	      b = arg2.getLocalName();
	    } 
	    else {
	      System.out.println("Error: Node types unequal.");
	      System.out.println(arg1.toString());
	      System.out.println(arg2.toString());
	    }
	
	    MetricLCS lcss = new MetricLCS();
	    double sim = 1-lcss.distance(a, b);
	
	    NodeValue nv = NodeValue.makeDouble(sim);
	    Binding bind = BindingFactory.binding(binding, Var.alloc(s), nv.asNode());
	    QueryIterSingleton q = QueryIterSingleton.create(bind, execCxt);
	    return q;
	}
}
