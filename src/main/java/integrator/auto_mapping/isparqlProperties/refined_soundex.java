package integrator.auto_mapping.isparqlProperties;

import simpack.accessor.string.StringAccessor;
import simpack.measure.sequence.Levenshtein;
import simpack.util.conversion.WorstCaseDistanceConversion;
import info.debatty.java.stringsimilarity.*;
import integrator.auto_mapping.String_transformation;

import org.apache.commons.codec.StringEncoderComparator;
import org.apache.commons.codec.language.RefinedSoundex;
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

public class refined_soundex extends PropertyFunctionBase {
  
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
	    Node arg3 = object.getArg(2);
	    
	    String a = null, b = null, c = null;
	    if (arg1.isLiteral() && arg2.isLiteral()) {
	      a = arg1.getLiteralLexicalForm();
	      b = arg2.getLiteralLexicalForm();
	    } 
	    if (arg3.isURI()) {
    	  c = arg3.getURI().toString();
  	    }
	    if (arg3.isLiteral()) {
	      c = arg3.getLiteralLexicalForm();
	    }
	    
	    String_transformation st = new String_transformation();
	    b = st.remove_identifier(c, b);
	    System.err.println(c);
	    
	    RefinedSoundex sndx = new RefinedSoundex();
	    MetricLCS lcss = new MetricLCS();
	    Double sim = (1-(lcss.distance(sndx.encode(a), sndx.encode(b))));
	    
	    NodeValue nv = NodeValue.makeDouble(sim);
	    Binding bind = BindingFactory.binding(binding, Var.alloc(s), nv.asNode());
	    QueryIterSingleton q = QueryIterSingleton.create(bind, execCxt);
	    return q;
	}
}