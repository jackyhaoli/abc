package abc.weaving.matching;

import java.util.*;

import soot.*;
import soot.jimple.*;
import soot.tagkit.Host;

import abc.weaving.aspectinfo.AbstractAdviceDecl;
import abc.weaving.residues.Residue;
import abc.weaving.residues.ContextValue;
import abc.weaving.residues.JimpleValue;

/** The results of matching at a field get
 *  @author Ganesh Sittampalam
 *  @date 05-May-04
 */
public class GetFieldShadowMatch extends StmtShadowMatch {
    
    private SootField field;
    
    private GetFieldShadowMatch(SootMethod container,Stmt stmt,SootField field) {
	super(container,stmt);
	this.field=field;
    }

    public SootField getField() {
	return field;
    }

    public static GetFieldShadowMatch matchesAt(MethodPosition pos) {
	if(!(pos instanceof StmtMethodPosition)) return null;
	if(abc.main.Debug.v().traceMatcher) System.err.println("GetField");

	Stmt stmt=((StmtMethodPosition) pos).getStmt();

	if (!(stmt instanceof AssignStmt)) return null;
	AssignStmt as = (AssignStmt) stmt;
	Value rhs = as.getRightOp();
       	if(rhs instanceof FieldRef) {
	    FieldRef fr = (FieldRef) rhs;

	    return new GetFieldShadowMatch(pos.getContainer(),stmt,fr.getField());
	    /*
	} else if(rhs instanceof InvokeExpr) {
	    if(MethodCategory.getCategory(rhs.getMethod())
	       ==MethodCategory.ACCESSOR_GET) {
		
		return new GetFieldShadowMatch(pos.getContainer(),stmt,null);
	    }
	    */
	} else {
	    return null;
	}
    }

    public Host getHost() {
	return stmt;
    }
    
    public SJPInfo makeSJPInfo() {
	return new SJPInfo
	    ("field-get","FieldSignature","makeFieldSig",
	     SJPInfo.makeFieldSigData(container,field),stmt);
    }


    protected AdviceApplication doAddAdviceApplication
	(MethodAdviceList mal,AbstractAdviceDecl ad,Residue residue) {

	StmtAdviceApplication aa=new StmtAdviceApplication(ad,residue,stmt);
	mal.addStmtAdvice(aa);
	return aa;
    }

    public ContextValue getTargetContextValue() {
	FieldRef fr=(FieldRef) (((AssignStmt) stmt).getRightOp());
	if(!(fr instanceof InstanceFieldRef)) return null;
	InstanceFieldRef ifr=(InstanceFieldRef) fr;
	return new JimpleValue(ifr.getBase());
    }

    public ContextValue getReturningContextValue() {
	return new JimpleValue(((AssignStmt) stmt).getLeftOp());
    }

    public List/*<ContextValue>*/ getArgsContextValues() {
	return new ArrayList(0);
    }

}
