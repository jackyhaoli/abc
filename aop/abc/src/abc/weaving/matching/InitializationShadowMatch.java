package abc.weaving.matching;

import soot.*;

import abc.weaving.aspectinfo.AdviceDecl;
import abc.weaving.residues.Residue;

/** The results of matching at an initialization shadow
 *  @author Ganesh Sittampalam
 *  @date 05-May-04
 */
public class InitializationShadowMatch extends ShadowMatch {
    private SootMethod container;

    private InitializationShadowMatch(SootMethod container) {
	this.container=container;
    }

    public ShadowMatch getEnclosing() {
	return this;
    }

    public static InitializationShadowMatch matchesAt(MethodPosition pos) {
	if(!(pos instanceof WholeMethodPosition)) return null;
	SootMethod container=pos.getContainer();
	if(!container.getName().equals(SootMethod.constructorName)) return null;
	return new InitializationShadowMatch(container);
    }


    public AdviceApplication.SJPInfo makeSJPInfo() {
	return new AdviceApplication.SJPInfo
	    ("initialization","ConstructorSignature","makeConstructorSig","",container);
    }

    protected AdviceApplication doAddAdviceApplication
	(MethodAdviceList mal,AdviceDecl ad,Residue residue) {

	InitializationAdviceApplication aa
	    =new InitializationAdviceApplication(ad,residue);
	mal.addInitializationAdvice(aa);
	return aa;
    }
}
