package org.jastadd.plugin.editor.highlight;

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class JastAddColors {
	private HashMap<RGB,Color> colors = new HashMap<RGB,Color>();
	
	public void dispose() {
		Iterator iter = colors.values().iterator();
		while(iter.hasNext()) {
			Color color = (Color)iter.next();
			color.dispose();
		}
		
	}
	
	public Color get(RGB rgb) {
		Color color = (Color)colors.get(rgb);
		if(color == null) {
			color = new Color(Display.getCurrent(), rgb);
			colors.put(rgb, color);
		}
		return color;
	}
}
