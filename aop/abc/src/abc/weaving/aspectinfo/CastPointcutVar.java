/* Abc - The AspectBench Compiler
 * Copyright (C) 2004 Ganesh Sittampalam
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


/** Cast from one pointcut variable to another. 
 *  This can appear after inlining
 *  @author Ganesh Sittampalam
 */
public class CastPointcutVar extends Pointcut {
    private Var from;
    private Var to;

    public CastPointcutVar(Var from,Var to,Position pos) {
	super(pos);
	this.from=from;
	this.to=to;
    }
    

    public Var getFrom() {
	return from;
    }

    public Var getTo() {
	return to;
    }

    public String toString() {
	return "cast("+from+","+to+")";
    }

    public Residue matchesAt(WeavingEnv we,
			     SootClass cls,
			     SootMethod method,
			     ShadowMatch sm) {
	Type fromType=we.getAbcType(from).getSootType();
	Type toType=we.getAbcType(to).getSootType();
	if(fromType instanceof PrimType && 
	   toType.equals(Scene.v().getSootClass("java.lang.Object").getType()))
	    return new Box(we.getWeavingVar(from),we.getWeavingVar(to));
	
	// no need to cast, because the rules guarantee this is an upcast...
	return new Copy(we.getWeavingVar(from),we.getWeavingVar(to));
    }

    protected Pointcut inline(Hashtable renameEnv,
			      Hashtable typeEnv,
			      Aspect context) {
	Var from=this.from;
	if(renameEnv.containsKey(from.getName()))
	   from=(Var) renameEnv.get(from.getName());

	Var to=this.to;
	if(renameEnv.containsKey(to.getName()))
	   to=(Var) renameEnv.get(to.getName());

	if(from != this.from || to != this.to)
	    return new CastPointcutVar(from,to,getPosition());
	else return this;
	   
    }
    public void registerSetupAdvice(Aspect context,Hashtable typeMap) {}
    public void getFreeVars(Set/*<String>*/ result) {
	result.add(to.getName());
    }

	/* (non-Javadoc)
	 * @see abc.weaving.aspectinfo.Pointcut#equivalent(abc.weaving.aspectinfo.Pointcut, java.util.Hashtable)
	 */
	public boolean canRenameTo(Pointcut otherpc, Hashtable renaming) {
		if (otherpc.getClass() == this.getClass()) {
			CastPointcutVar othcast = (CastPointcutVar) otherpc;
			
			if (from.canRenameTo(othcast.getFrom(), renaming)) {
				return (to.canRenameTo(othcast.getTo(), renaming));
			} else return false;
		} else return false;
	}

}
