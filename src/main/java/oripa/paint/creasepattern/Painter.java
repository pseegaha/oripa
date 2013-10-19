package oripa.paint.creasepattern;

import java.util.Collection;

import oripa.paint.creasepattern.command.LineAdder;
import oripa.paint.creasepattern.command.LineMirror;
import oripa.value.OriLine;

public class Painter {

	/**
	 * reset selection of all lines in given collection.
	 * @param creasePattern
	 */
	public void resetSelectedOriLines(Collection<OriLine> creasePattern) {
		for (OriLine line : creasePattern) {
			line.selected = false;
		}
	}

	/**
	 * select all lines in given collection.
	 * @param creasePattern
	 */
	public void selectAllOriLines(Collection<OriLine> creasePattern) {
		for (OriLine l : creasePattern) {
			if (l.typeVal != OriLine.TYPE_CUT) {
				l.selected = true;
			}
		}
	}


	/**
	 * Adds a new OriLine, also searching for intersections with others 
	 * that would cause their mutual division.
	 * @param inputLine      a line to be added
	 * @param creasePattern  destination of inputLine
	 */
	public void addLine(OriLine inputLine, Collection<OriLine> creasePattern) {
		LineAdder lineAdder = new LineAdder();
		
		lineAdder.addLine(inputLine, creasePattern);		
	}

	/**
	 * 
	 * 
	 * @param baseLine       a line to be the axis of symmetry
	 * @param lines          lines to be mirrored
	 * @param creasePattern  destination of mirrored lines
	 */
	public void mirrorCopyBy(OriLine baseLine,
			Collection<OriLine> lines, Collection<OriLine> creasePattern) {

		LineMirror mirror = new LineMirror();
		Collection<OriLine> copiedLines = mirror.createMirroredLines(baseLine, lines);

		for (OriLine line : copiedLines) {
			addLine(line, creasePattern);
		}

	}    

}