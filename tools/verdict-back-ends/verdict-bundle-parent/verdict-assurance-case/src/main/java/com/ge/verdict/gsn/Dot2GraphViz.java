package com.ge.verdict.gsn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;

/**
 * A wrapper for the GraphViz API 
 * @author Saswata Paul
 *
 */
public class Dot2GraphViz {

	/**
	 * Takes the address strings of a source dot file and a destination svg file
	 * and creates an svg file from the dot using the GraphViz API
	 * @param sourceAddress
	 * @param outputAddress
	 * @throws IOException
	 * @throws IOException
	 */
	public static void generateGraph(String sourceAddress, String outputAddress) throws IOException, IOException {
        // GraphViz parse dot file example
        try (InputStream dot = new FileInputStream(sourceAddress); ) {
            MutableGraph g = new Parser().read(dot);
            Graphviz.fromGraph(g)
            		.width(1400)
                    .render(Format.SVG)
                    .toFile(new File(outputAddress));
        }
	}
}
