/**
 * Natural Language Processing package
 */
package kr.jihee.irnlp_toolkit.nlp;

import java.io.*;
import java.util.*;

import kr.jihee.text_toolkit.io.*;
import opennlp.tools.chunker.*;
import opennlp.tools.cmdline.parser.*;
import opennlp.tools.namefind.*;
import opennlp.tools.parser.*;
import opennlp.tools.postag.*;
import opennlp.tools.sentdetect.*;
import opennlp.tools.tokenize.*;
import opennlp.tools.util.*;

/**
 * Wrapper of OpenNLP 1.5.3<br>
 * = URL : http://opennlp.apache.org/
 * 
 * @author Jihee
 */
public class OpenNlpWrapper {

	public Properties prop;
	public SentenceDetectorME detector;
	public TokenizerME tokenizer;
	public POSTaggerME tagger;
	public ChunkerME chunker;
	public Parser parser;
	public List<NameFinderME> recognizers;

	public OpenNlpWrapper(String path) throws IOException {
		prop = new Properties();
		prop.loadFromXML(new FileInputStream(JFile.asFile(path)));
	}

	public OpenNlpWrapper loadSentDetector() throws IOException {
		String model_path = prop.getProperty("sent.model");
		System.err.printf("Loading sentence detector from %s ... ", model_path);
		detector = new SentenceDetectorME(new SentenceModel(JFile.asStream(model_path)));
		System.err.println("done");
		return this;
	}

	public OpenNlpWrapper loadTokenizer() throws IOException {
		String model_path = prop.getProperty("tok.model");
		System.err.printf("Loading tokenizer from %s ... ", model_path);
		tokenizer = new TokenizerME(new TokenizerModel(JFile.asStream(model_path)));
		System.err.println("done");
		return this;
	}

	public OpenNlpWrapper loadPosTagger() throws IOException {
		String model_path = prop.getProperty("pos.model");
		System.err.printf("Loading POS tagger from %s ... ", model_path);
		tagger = new POSTaggerME(new POSModel(JFile.asStream(model_path)));
		System.err.println("done");
		return this;
	}

	public OpenNlpWrapper loadChunker() throws IOException {
		String model_path = prop.getProperty("chunk.model");
		System.err.printf("Loading phrase chunker from %s ... ", model_path);
		chunker = new ChunkerME(new ChunkerModel(JFile.asStream(model_path)));
		System.err.println("done");
		return this;
	}

	public OpenNlpWrapper loadLexParser() throws IOException {
		String model_path = prop.getProperty("parse.model");
		System.err.printf("Loading parser from %s ... ", model_path);
		parser = ParserFactory.create(new ParserModel(JFile.asStream(model_path)));
		System.err.println("done");
		return this;
	}

	public OpenNlpWrapper loadEntityRecognizers() throws IOException {
		recognizers = new ArrayList<NameFinderME>();
		for (String key : Arrays.asList("ner.person.model", "ner.organization.model", "ner.location.model", "ner.date.model", "ner.time.model", "ner.money.model", "ner.percentage.model")) {
			String model_path = prop.getProperty(key);
			System.err.printf("Loading named entity recognizer from %s ... ", model_path);
			recognizers.add(new NameFinderME(new TokenNameFinderModel(JFile.asStream(model_path))));
			System.err.println("done");
		}
		return this;
	}

	public OpenNlpWrapper loadAll() throws IOException {
		loadAll("ssplit, tokenize, pos, chunk, parse, ner");
		return this;
	}

	public OpenNlpWrapper loadAll(String annotator_spec) throws IOException {
		List<String> annotators = Arrays.asList(annotator_spec.toLowerCase().replaceAll("\\s", "").split(","));
		if (annotators.contains("ssplit"))
			loadSentDetector();
		if (annotators.contains("tokenize"))
			loadTokenizer();
		if (annotators.contains("pos"))
			loadPosTagger();
		if (annotators.contains("chunk"))
			loadChunker();
		if (annotators.contains("parse"))
			loadLexParser();
		if (annotators.contains("ner"))
			loadEntityRecognizers();
		return this;
	}

	public String[] tokenize(String text) {
		return tokenizer.tokenize(text);
	}

	public String[] detect(String text) {
		return detector.sentDetect(text);
	}

	public String[] tag(String[] toks) {
		return tagger.tag(toks);
	}

	public String[] chunk(String[] toks, String[] tags) {
		return chunker.chunk(toks, tags);
	}

	public Parse parse(String sent) {
		return ParserTool.parseLine(sent, parser, 1)[0];
	}

	public Parse[] parse(String sent, int k) {
		return ParserTool.parseLine(sent, parser, k);
	}

	public List<Span> recognize(String[] toks) {
		ArrayList<Span> spans = new ArrayList<Span>();
		for (NameFinderME recognizer : recognizers)
			for (Span s : recognizer.find(toks))
				spans.add(s);
		return spans;
	}

	public void clearRecognizers() {
		for (NameFinderME recognizer : recognizers)
			recognizer.clearAdaptiveData();
	}

	public static String toChunkString(String[] toks, String[] tags, String[] chunks) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < chunks.length; i++) {
			if (i > 0 && !chunks[i].startsWith("I-") && !chunks[i - 1].equals("O"))
				sb.append(">");
			if (i > 0 && chunks[i].startsWith("B-"))
				sb.append(" <" + chunks[i].substring(2));
			else if (chunks[i].startsWith("B-"))
				sb.append("<" + chunks[i].substring(2));
			sb.append(" " + toks[i] + "/" + tags[i]);
		}
		if (!chunks[chunks.length - 1].equals("O"))
			sb.append(">");
		return sb.toString();
	}

	public static String toTreeString(Parse tree) {
		StringBuffer sb = new StringBuffer();
		tree.show(sb);
		return sb.toString();
	}
}
