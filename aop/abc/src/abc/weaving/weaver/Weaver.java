package abc.weaving.weaver;

import soot.*;
import soot.util.*;
import soot.jimple.*;
import java.util.*;
import abc.weaving.aspectinfo.*;
import abc.weaving.matching.*;

/** The driver for the weaving process.  
 * @author Jennifer Lhotak
 * @author Ondrej Lhotak
 * @author Laurie Hendren
 * @date April 24, 2004
 */

public class Weaver {

    /** set to false to disable debugging messages for ShadowPointsSetter */
    public static boolean debug = true;


    private static void debug(String message)
      { if (debug) System.err.println("WEAVER DRIVER ***** " + message);
      }	

    public void weave() {
        // Generate intertype methods and fields
        debug("Generating intertype methods and fields ....");
        IntertypeGenerator ig = new IntertypeGenerator();
	//  --- Intertype methods
        for( Iterator imdIt = 
	        GlobalAspectInfo.v().getIntertypeMethodDecls().iterator(); 
		imdIt.hasNext(); ) {
            final IntertypeMethodDecl imd = (IntertypeMethodDecl) imdIt.next();
            ig.addMethod( imd );
        }
        // --- Intertype fields
        for( Iterator ifdIt =
	        GlobalAspectInfo.v().getIntertypeFieldDecls().iterator(); 
		ifdIt.hasNext(); ) {
            final IntertypeFieldDecl ifd = (IntertypeFieldDecl) ifdIt.next();
            ig.addField( ifd );
        }

        // Generate methods inside aspects needed for code gen and bodies of
	//   methods not filled in by front-end (i.e. aspectOf())
	debug("Generating extra code in aspects");
        AspectCodeGen ag = new AspectCodeGen();
        for( Iterator asIt = 
	         GlobalAspectInfo.v().getAspects().iterator(); 
		 asIt.hasNext(); ) {
            final Aspect as = (Aspect) asIt.next();
            ag.fillInAspect(as.getInstanceClass().getSootClass());
        }

	ShadowPointsSetter sg = new ShadowPointsSetter();
        PointcutCodeGen pg = new PointcutCodeGen();
	GenStaticJoinPoints gsjp = new GenStaticJoinPoints();

        for( Iterator clIt = 
	         GlobalAspectInfo.v().getWeavableClasses().iterator(); 
		 clIt.hasNext(); ) {
            final AbcClass cl = (AbcClass) clIt.next();
	    final SootClass scl = cl.getSootClass();
	    debug("--------- STARTING WEAVING OF CLASS >>>>> " + scl.getName());
	    // generate the Static Join Points
	    gsjp.genStaticJoinPoints(scl);
	    // pass one, do not handle initialization and preinitialization
            sg.setShadowPointsPass1(scl);
            pg.weaveInAspectsPass(scl,1);
	    // pass two, handle initializaiton and preinititalization
	    sg.setShadowPointsPass2(scl);
	    pg.weaveInAspectsPass(scl,2);
	    debug("--------- FINISHED WEAVING OF CLASS >>>>> " + 
		  scl.getName() + "\n");
	} // each class

    } // method weave
} // class Weaver
