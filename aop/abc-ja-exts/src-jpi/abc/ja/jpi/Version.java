/* abc - The AspectBench Compiler
 * Copyright (C) 2010 Eric Bodden
 *
 * This compiler is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This compiler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this compiler, in the file LESSER-GPL;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package abc.ja.jpi;

/**
 * Version information for this extension
 * @author Milton Inostroza
 */
public class Version extends abc.aspectj.Version {
    public String name() { return "abc+ja+cjp+jpi"; }  
    
    @Override
    public int major() {
    	return 0;
    }
    
    @Override
    public int minor() {
    	return 9;
    }
    
    @Override
    public int patch_level() {
    	return 0;
    }
    
    @Override
    public String prerelease() {
    	return "";
    }
}
