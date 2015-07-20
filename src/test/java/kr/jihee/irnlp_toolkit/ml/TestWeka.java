package kr.jihee.irnlp_toolkit.ml;

import java.io.*;

import kr.jihee.text_toolkit.io.JText.*;

public class TestWeka {
	public static void main(String[] args) throws Exception {
		System.out.println(1);

		WekaWrapper.writeToArff(new File("annotation.csv"), new File("annotation.arff"));
		WekaWrapper.classify(new File("annotation.csv"), new File("smo.model"), new File("temp.arff"), new File("annotation2.csv"));
		TextWriter.write(new File("annotation3.csv"), TextReader.read(new File("annotation2.csv")).replace("'", "\""));

		System.out.println(2);
	}
}
