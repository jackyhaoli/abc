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
import polyglot.types.SemanticException;

import soot.*;
import soot.jimple.*;

import abc.weaving.matching.*;
import abc.weaving.residues.*;

/** Pointcut disjunction. 
 *  @author Aske Simon Christensen
 *  @author Ganesh Sittampalam
 *  @author Damien Sereni
 */
public class OrPointcut extends Pointcut {
    private Pointcut pc1;
    private Pointcut pc2;

    private OrPointcut(Pointcut pc1, Pointcut pc2, Position pos) {
	super(pos);
	this.pc1 = pc1;
	this.pc2 = pc2;
    }

    public static Pointcut construct(Pointcut pc1, Pointcut pc2, Position pos) {
	if(pc2 instanceof EmptyPointcut || pc1 instanceof FullPointcut) return pc1;
	if(pc1 instanceof EmptyPointcut || pc2 instanceof FullPointcut) return pc2;
	return new OrPointcut(pc1,pc2,pos);
    }

    public Pointcut getLeftPointcut() {
	return pc1;
    }

    public Pointcut getRightPointcut() {
	return pc2;
    }

    public Residue matchesAt(WeavingEnv we,
			     SootClass cls,
			     SootMethod method,
			     ShadowMatch sm) 
	throws SemanticException
    {
	return OrResidue.construct(pc1.matchesAt(we,cls,method,sm),
				   pc2.matchesAt(we,cls,method,sm));
    }

    protected Pointcut inline(Hashtable renameEnv,Hashtable typeEnv,Aspect context) {
	Pointcut pc1=this.pc1.inline(renameEnv,typeEnv,context);
	Pointcut pc2=this.pc2.inline(renameEnv,typeEnv,context);
	if(pc1==this.pc1 && pc2==this.pc2) return this;
	else return construct(pc1,pc2,getPosition());
    }

    protected DNF dnf() {
	DNF dnf1=pc1.dnf();
	DNF dnf2=pc2.dnf();
	return DNF.or(dnf1,dnf2);
    }

    public String toString() {
	return "("+pc1+") || ("+pc2+")";
    }

    public void registerSetupAdvice(Aspect context,Hashtable typeMap) {
	pc1.registerSetupAdvice(context,typeMap);
	pc2.registerSetupAdvice(context,typeMap);
    }


    public void getFreeVars(Set result) {
	pc1.getFreeVars(result);
	pc2.getFreeVars(result);
    }

	/* (non-Javadoc)
	 * @see abc.weaving.aspectinfo.Pointcut#equivalent(abc.weaving.aspectinfo.Pointcut, java.util.Hashtable)
	 */
	public boolean canRenameTo(Pointcut otherpc, Hashtable renaming) {
		if (otherpc.getClass() == this.getClass()) {
			Pointcut otherpc1 = ((OrPointcut)otherpc).getLeftPointcut();
			Pointcut otherpc2 = ((OrPointcut)otherpc).getRightPointcut();
			
			// Bound vars on both sides of the or are the same
			// Can find subst from the left pc, and check that it works
			// the right pc
			// This is done by checking whenever a var is added to the subst
			// that it is either new, or that the new binding is the same as
			// the existing one (ie never override bindings)
			
			if (pc1.canRenameTo(otherpc1, renaming)) {
				return pc2.canRenameTo(otherpc2, renaming);
			} else return false;
		} else return false;
	}

}
