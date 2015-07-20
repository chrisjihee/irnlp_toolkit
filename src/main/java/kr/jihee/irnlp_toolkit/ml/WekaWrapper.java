package kr.jihee.irnlp_toolkit.ml;

import java.io.*;

import weka.classifiers.*;
import weka.core.*;
import weka.core.converters.*;
import weka.core.converters.ConverterUtils.*;

/**
 * Wrapper of Weka 3.7.12<br>
 * - URL : http://www.cs.waikato.ac.nz/ml/weka/
 * 
 * @author Jihee
 */
public class WekaWrapper {

	public static void writeToArff(File csv, File arff) throws Exception {
		CSVLoader loader = new CSVLoader();
		loader.setSource(csv);
		Instances data = loader.getDataSet();
		data.setClassIndex(data.numAttributes() - 1);
		DataSink.write(arff.getPath(), data);
	}

	public static void classify(File inp, File model, File temp, File out) throws Exception {
		AbstractClassifier classifier = null;
		try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(model))) {
			Object obj = is.readObject();
			if (obj instanceof AbstractClassifier)
				classifier = (AbstractClassifier) obj;
			else
				throw new Exception("Not a valid classifier model!");
		}
		WekaWrapper.writeToArff(inp, temp);
		Instances unlabeled = DataSource.read(temp.getPath());
		unlabeled.setClassIndex(unlabeled.numAttributes() - 1);
		Instances labeled = new Instances(unlabeled);
		for (int i = 0; i < unlabeled.numInstances(); i++) {
			double label = classifier.classifyInstance(unlabeled.instance(i));
			labeled.instance(i).setClassValue(label);
		}
		DataSink.write(out.getPath(), labeled);
	}
}
