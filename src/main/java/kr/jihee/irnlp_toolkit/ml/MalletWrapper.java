/**
 * Machine Learning package
 */
package kr.jihee.irnlp_toolkit.ml;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.function.*;
import java.util.regex.*;
import java.util.stream.*;

import cc.mallet.fst.*;
import cc.mallet.fst.SimpleTagger.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;
import cc.mallet.types.*;

/**
 * Wrapper of Mallet 2.0.7<br>
 * - URL : http://mallet.cs.umass.edu/
 * 
 * @author Jihee
 */
public class MalletWrapper {

	public MalletWrapper() {
		System.setProperty("java.util.logging.config.file", "");
	}

	/**
	 * Wrapper of CRF in Mallet
	 * 
	 * @author Jihee
	 */
	public static class MalletCRFWrapper extends MalletWrapper {

		public static double DEFAULT_PRIOR_VARIANCE = 10.0;
		public static String DEFAULT_LABEL = "O";
		public static boolean DEFAULT_TARGET_PROCESSING = true;

		public InstanceList train_data;
		public InstanceList test_data;
		public CRF model;

		/**
		 * 
		 * @param data_file
		 * @throws FileNotFoundException
		 */
		public void setTrainData(String data_file) throws FileNotFoundException {
			this.train_data = getInstanceList(data_file);
		}

		/**
		 * 
		 * @param data
		 */
		public void setTrainData(String[] data) {
			this.train_data = getInstanceList(data);
		}

		/**
		 * 
		 * @param data_file
		 * @throws FileNotFoundException
		 */
		public void setTestData(String data_file) throws FileNotFoundException {
			this.test_data = getInstanceList(data_file);
		}

		/**
		 * 
		 * @param data
		 */
		public void setTestData(String[] data) {
			this.test_data = getInstanceList(data);
		}

		/**
		 * 
		 * @param data_file
		 * @return
		 * @throws FileNotFoundException
		 */
		public InstanceList getInstanceList(String data_file) throws FileNotFoundException {
			InstanceList instances = new InstanceList(getPipe());
			instances.addThruPipe(new LineGroupIterator(new FileReader(new File(data_file)), Pattern.compile("^\\s*$"), true));
			return instances;
		}

		/**
		 * 
		 * @param data
		 * @return
		 */
		public InstanceList getInstanceList(String[] data) {
			InstanceList instances = new InstanceList(getPipe());
			instances.addThruPipe(new StringArrayIterator(data));
			return instances;
		}

		/**
		 * 
		 * @return
		 */
		private Pipe getPipe() {
			Pipe p = (this.model == null) ? new SimpleTaggerSentence2FeatureVectorSequence() : this.model.getInputPipe();
			p.setTargetProcessing(DEFAULT_TARGET_PROCESSING);
			return p;
		}

		/**
		 * 
		 * @param num_iterations
		 * @return
		 */
		public Boolean train(Integer num_iterations) {
			this.model = new CRF(this.train_data.getPipe(), (Pipe) null);
			for (int i = 0; i < this.model.numStates(); i++)
				this.model.getState(i).setInitialWeight(Transducer.IMPOSSIBLE_WEIGHT);
			String startName = this.model.addOrderNStates(this.train_data, new int[] { 1 }, null, DEFAULT_LABEL, Pattern.compile("\\s"), Pattern.compile(".*"), true);
			this.model.getState(startName).setInitialWeight(0.0);

			CRFTrainerByLabelLikelihood crft = new CRFTrainerByLabelLikelihood(this.model);
			crft.setGaussianPriorVariance(DEFAULT_PRIOR_VARIANCE);
			crft.setUseSparseWeights(true);
			crft.setUseSomeUnsupportedTrick(true);

			for (int i = 0; i < num_iterations; i++)
				if (crft.train(this.train_data, 1))
					break;

			return this.model != null;
		}

		/**
		 * 
		 * @param num_best
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public List<CRFResult> test(Integer num_best) {
			List<CRFResult> groups = new ArrayList<CRFResult>();
			for (Instance instance : this.test_data) {
				FeatureVectorSequence input = (FeatureVectorSequence) instance.getData();
				List<Sequence<Object>> seqs = null;
				if (num_best > 1)
					seqs = new MaxLatticeDefault(model, input, null, 100000).bestOutputSequences(num_best);
				else
					seqs = Arrays.asList((Sequence<Object>) model.transduce(input));
				if (isError(seqs, input))
					System.err.println("[ERROR] Error output at " + input);

				List<List<String>> outputs = new ArrayList<List<String>>();
				for (Sequence<Object> seq : seqs)
					outputs.add(toTagStrings(seq));

				groups.add(new CRFResult(input, outputs));
			}

			return groups;
		}

		/**
		 * 
		 * @param seqs
		 * @param input
		 * @return
		 */
		private boolean isError(List<Sequence<Object>> seqs, FeatureVectorSequence input) {
			for (Sequence<Object> seq : seqs)
				if (seq.size() != input.size())
					return true;
			return false;
		}

		/**
		 * Transform a sequence output into a list of tag strings
		 * 
		 * @param output
		 * @return
		 */
		public static List<String> toTagStrings(Sequence<Object> output) {
			List<String> tags = new ArrayList<String>();
			for (int i = 0; i < output.size(); i++)
				tags.add(output.get(i).toString());
			return tags;
		}
	}

	/**
	 * Wrapper of LDA in Mallet
	 * 
	 * @author Jihee
	 */
	public static class MalletLDAWrapper extends MalletWrapper {

		public static int DEFAULT_NUM_KEYWORDS = 20;
		public static double DEFAULT_ALPHA_SUM = 50.0;
		public static double DEFAULT_BETA = 0.01;
		public static Function<Entry<String, String>, Instance> instance_mapper = x -> new Instance(x.getValue(), "MyTarget", x.getKey(), null);

		public InstanceList data;
		public ParallelTopicModel model;

		public TreeMap<Integer, ArrayList<String>> clusteredWords;

		/**
		 * 
		 * @param data_dir
		 */
		public void setInputData(String data_dir) {
			this.data = getInstanceList(data_dir);
		}

		/**
		 * 
		 * @param data
		 */
		public void setInputData(LinkedHashMap<String, String> data) {
			this.data = getInstanceList(data);
		}

		/**
		 * 
		 * @param data_dir
		 * @return
		 */
		public InstanceList getInstanceList(String data_dir) {
			InstanceList instances = new InstanceList(getPipe());
			instances.addThruPipe(new FileIterator(new File[] { new File(data_dir) }, FileIterator.STARTING_DIRECTORIES, true));
			return instances;
		}

		/**
		 * 
		 * @param data
		 * @param model
		 * @return
		 */
		public InstanceList getInstanceList(LinkedHashMap<String, String> data) {
			InstanceList instances = new InstanceList(getPipe());
			List<Instance> collected = data.entrySet().parallelStream().map(instance_mapper).collect(Collectors.toList());
			instances.addThruPipe(collected.iterator());
			return instances;
		}

		/**
		 * 
		 * @param model
		 * @param targetProcessing
		 * @return
		 */
		private Pipe getPipe() {
			ArrayList<Pipe> pipes = new ArrayList<Pipe>();
			pipes.add(new Target2Label());
			pipes.add(new SaveDataInSource());
			pipes.add(new Input2CharSequence("UTF-8"));
			pipes.add(new CharSequence2TokenSequence(Pattern.compile("\\p{Alpha}+")));
			pipes.add(new TokenSequenceLowercase());
			pipes.add(new TokenSequenceRemoveStopwords(false, false));
			pipes.add(new TokenSequence2FeatureSequence());
			// pipes.add(new PrintInputAndTarget());
			return new SerialPipes(pipes);
		}

		/**
		 * 
		 * @param num_clusters
		 * @param num_iterations
		 * @return
		 * @throws IOException
		 */
		public List<LDAResult> cluster(int num_clusters, int num_iterations) throws IOException {
			model = new ParallelTopicModel(num_clusters, DEFAULT_ALPHA_SUM, DEFAULT_BETA);
			model.addInstances(this.data);
			model.setTopicDisplay(500, DEFAULT_NUM_KEYWORDS);
			model.setNumIterations(num_iterations);
			model.setOptimizeInterval(0);
			model.setBurninPeriod(200);
			model.setSymmetricAlpha(false);
			model.setNumThreads(1);
			model.estimate();

			ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
			clusteredWords = new TreeMap<Integer, ArrayList<String>>();
			for (int topic = 0; topic < num_clusters; topic++) {
				ArrayList<String> words = new ArrayList<String>();
				for (IDSorter info : topicSortedWords.get(topic))
					words.add(String.format("%s/%.0f", model.alphabet.lookupObject(info.getID()), info.getWeight()));
				clusteredWords.put(topic, words);
			}

			IDSorter[] outputs = new IDSorter[num_clusters];
			List<LDAResult> groups = new ArrayList<LDAResult>();
			for (int id = 0; id < model.data.size(); id++) {
				TopicAssignment result = model.data.get(id);
				String name = result.instance.getName().toString();
				FeatureSequence input = (FeatureSequence) result.instance.getData();

				TreeMap<Integer, Integer> topic_counts = new TreeMap<Integer, Integer>();
				for (int topic = 0; topic < num_clusters; topic++)
					topic_counts.put(topic, 0);
				for (int topic : result.topicSequence.getFeatures())
					topic_counts.put(topic, topic_counts.get(topic) + 1);

				for (int topic = 0; topic < num_clusters; topic++) {
					double prob = (topic_counts.get(topic) + model.alpha[topic]) / (result.topicSequence.getFeatures().length + model.alphaSum);
					outputs[topic] = new IDSorter(topic, prob);
				}
				Arrays.sort(outputs);

				groups.add(new LDAResult(id, name, input, outputs));
			}

			return groups;
		}
	}

	/**
	 * Result item of CRF
	 * 
	 * @author Jihee
	 */
	public static class CRFResult {
		public FeatureVectorSequence input;
		public List<List<String>> outputs;

		public CRFResult(FeatureVectorSequence input, List<List<String>> outputs) {
			this.input = input;
			this.outputs = outputs;
		}
	}

	/**
	 * Result item of LDA
	 * 
	 * @author Jihee
	 */
	public static class LDAResult {
		public Integer id;
		public String name;
		public FeatureSequence input;
		public List<IDSorter> outputs;

		public LDAResult(int id, String name, FeatureSequence input, IDSorter[] outputs) {
			this.id = id;
			this.name = name;
			this.input = input;
			this.outputs = new ArrayList<IDSorter>();
			for (IDSorter output : outputs)
				this.outputs.add(new IDSorter(output.getID(), output.getWeight()));
		}
	}
}
