/* Abc - The AspectBench Compiler
 * Copyright (C) 2004 Aske Simon Christensen
 * Copyright (C) 2004 Ganesh Sittampalam
 * Copyright (C) 2004 Damien Sereni
 *
 * This compiler is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this compiler, in the file LESSER-GPL; 
 * if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package abc.weaving.aspectinfo;

import java.util.*;

import polyglot.util.Position;
import soot.*;
import abc.weaving.matching.*;
import abc.weaving.residues.*;

/** Handler for <code>if</code> condition pointcut. 
 *  @author Aske Simon Christensen
 *  @author Ganesh Sittampalam
 *  @author Damien Sereni
 */
public class If extends Pointcut {
    private List/*<Var>*/ vars;
    private MethodSig impl;

    int jp,jpsp,ejp;

    public If(List vars, MethodSig impl, int jp, int jpsp, int ejp, Position pos) {
	super(pos);
	this.vars = vars;
	this.impl = impl;
	
	this.jp = jp;
	this.jpsp = jpsp;
	this.ejp = ejp;
    }

    public boolean hasJoinPoint() {
	return jp != -1;
    }

    public boolean hasJoinPointStaticPart() {
	return jpsp != -1;
    }

    public boolean hasEnclosingJoinPoint() {
	return ejp != -1;
    }

    public int joinPointPos() {
	return jp;
    }

    public int joinPointStaticPartPos() {
	return jpsp;
    }

    public int enclosingJoinPointPos() {
	return ejp;
    }


    /** Get the pointcut variables that should be given as arguments to
     *  the method implementing the <code>if</code> condition.
     *  @return a list of {@link abc.weaving.aspectinfo.Var} objects.
     */
    public List getVars() {
	return vars;
    }

    /** Get the signature of the method implementing
     *  the <code>if</code> condition.
     */
    public MethodSig getImpl() {
	return impl;
    }

    public String toString() {
	return "if(...)";
    }

    public Residue matchesAt(WeavingEnv we,SootClass cls,SootMethod method,ShadowMatch sm) {
	Residue ret=AlwaysMatch.v;

	List/*<WeavingVar>*/ args=new LinkedList();
	Iterator it=vars.iterator();
	int i=0;
	while(it.hasNext()) {
	    WeavingVar wvar;
	    Var var=(Var) it.next();

	    if(i==joinPointStaticPartPos()) {
		wvar=new LocalVar(RefType.v("org.aspectj.lang.JoinPoint$StaticPart"),
				 "thisJoinPointStaticPart");
		ret=AndResidue.construct
		    (ret,new Load(new StaticJoinPointInfo(sm.getSJPInfo()),wvar));
	    } else if(i==enclosingJoinPointPos()) {
		wvar=new LocalVar(RefType.v("org.aspectj.lang.JoinPoint$StaticPart"),
				 "thisEnclosingJoinPointStaticPart");
		ret=AndResidue.construct
		    (ret,new Load(new StaticJoinPointInfo(sm.getEnclosing().getSJPInfo()),wvar));
	    } else if(i==joinPointPos()) {
		wvar=new LocalVar(RefType.v("org.aspectj.lang.JoinPoint"),
				 "thisJoinPoint");
		ret=AndResidue.construct
		    (ret,new Load(new JoinPointInfo(sm),wvar));

		// make sure the SJP info will be around later for 
		// the JoinPointInfo residue
		sm.recordSJPInfo(); 
	    } else wvar=we.getWeavingVar(var);

	    args.add(wvar);
	    i++;
	}
	ret=AndResidue.construct(ret,IfResidue.construct(impl.getSootMethod(),args));
	return ret;
    }

    protected Pointcut inline(Hashtable renameEnv,
			      Hashtable typeEnv,
			      Aspect context) {
	Iterator it=vars.iterator();
	List newvars=new LinkedList();
	while(it.hasNext())
	    newvars.add(((Var) it.next()).rename(renameEnv));
	return new If(newvars,impl,jp,jpsp,ejp,getPosition());
    }

    public void registerSetupAdvice(Aspect context,Hashtable typeMap) {}
    public void getFreeVars(Set/*<String>*/ result) {
	// just want binding occurrences, so do nothing
    }

	/* (non-Javadoc)
	 * @see abc.weaving.aspectinfo.Pointcut#equivalent(abc.weaving.aspectinfo.Pointcut, java.util.Hashtable)
	 */
	public boolean canRenameTo(Pointcut otherpc, Hashtable renaming) {
		if (otherpc.getClass() == this.getClass()) {
			If oif = (If)otherpc;
			
			if (this.hasJoinPoint() != oif.hasJoinPoint()) return false;
			if (this.hasJoinPointStaticPart() != oif.hasJoinPointStaticPart()) return false;
			if (this.hasEnclosingJoinPoint() != oif.hasEnclosingJoinPoint()) return false;
			
			// COMPARING VARS
			
			Iterator it1 = vars.iterator();
			Iterator it2 = oif.getVars().iterator();
			
			while (it1.hasNext() && it2.hasNext()) {
				Var var1 = (Var) it1.next();
				Var var2 = (Var) it2.next();
				
				if (!var1.canRenameTo(var2, renaming)) return false;
			}
			if (it1.hasNext() || it2.hasNext()) return false;
			
			// COMPARING IMPLEMENTATIONS
			// FIXME Is it OK to require If methods to be equal regardless of renaming?
			// It seems that some substitution should be required inside the body
			// but inline() doesn't do that
			
			if (!impl.equals(oif.getImpl())) return false;
			
			return true;
		} else return false;
	}

}
