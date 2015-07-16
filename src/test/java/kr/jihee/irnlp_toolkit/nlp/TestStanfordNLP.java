/**
 * Natural Language Processing package
 */
package kr.jihee.irnlp_toolkit.nlp;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

import kr.jihee.text_toolkit.io.*;
import kr.jihee.text_toolkit.io.JXml.XmlReader;
import kr.jihee.text_toolkit.lang.*;

import org.apache.logging.log4j.*;
import org.junit.*;
import org.xml.sax.*;

import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.*;

/**
 * Unit test for functions using StanfordNLP
 * 
 * @author Jihee
 */
public class TestStanfordNLP {
	static Logger log = LogManager.getLogger(Test.class);
	static String path = "resources/StanfordNLP.xml";

	/**
	 * Tests for configuration
	 * 
	 * @throws IOException
	 */
	@Test
//	@Ignore
	public void testConfiguration() throws IOException {
		log.info("<START> testConfiguration()");

		File conf = JFile.asFile(path);
		if (conf != null) {
			System.out.println(String.format(">>> Configuration file: %s [%s]", conf.getCanonicalPath(), conf.canRead() ? "OK" : "NO"));
			assertTrue(conf.canRead());
		}
		assertNotNull(conf);

		log.info("<PASSED> testConfiguration()");
	}

	/**
	 * Tests for basic functions
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws XPathExpressionException
	 */
	@Test
	@Ignore
	public void testBasicFunction() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
		log.info("<START> testBasicFunction()");

		String text = "Samsung Electronics is a South Korean multinational electronics company headquartered in Suwon, South Korea.";
		text += " It is the flagship subsidiary of the Samsung Group.";

		StanfordNlpWrapper nlp = new StanfordNlpWrapper(path);
		nlp.loadPosTagger();
		nlp.loadLexParser();
		assertNotNull(nlp.tagger);
		assertNotNull(nlp.parser);

		for (List<HasWord> words : StanfordNlpWrapper.detect(text)) {
			System.out.println("\n[Sentence] " + JString.join(" ", words));
			System.out.println("  - Tagged : " + JString.join(" ", nlp.tag(words)));
			System.out.println("  - Parsed : " + StanfordNlpWrapper.toTreeString(nlp.parse(words), "oneline"));
			assertEquals(words.size(), nlp.tag(words).size());

			System.out.println("  - XMLTree : \n" + StanfordNlpWrapper.toTreeString(nlp.parse(words), "xmlTree"));
			XmlReader reader = XmlReader.of(StanfordNlpWrapper.toTreeString(nlp.parse(words), "xmlTree"));
			Integer num_vp = reader.count("node[@value='ROOT']/node[@value='S']/node[@value='VP']");
			System.out.println("  - num(VP) : " + num_vp);
			System.out.println();
			assertTrue(num_vp > 0);
		}
		assertTrue(StanfordNlpWrapper.detect(text).size() > 0);

		log.info("<PASSED> testBasicFunction()");
	}

	/**
	 * Tests for multiple parsing
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testMultipleParsing() throws IOException {
		log.info("<START> testMultipleParsing()");

		String text = "Power on your Kindle.";
		text += " Press the Home button to ensure you are at the list of downloaded books.";

		StanfordNlpWrapper nlp = new StanfordNlpWrapper(path);
		nlp.loadPosTagger();
		nlp.loadLexParser();
		assertNotNull(nlp.tagger);
		assertNotNull(nlp.parser);

		for (List<HasWord> words : StanfordNlpWrapper.detect(text)) {
			System.out.println("\n[Sentence] " + JString.join(" ", words));
			System.out.println("  - Tagged : " + JString.join(" ", nlp.tag(words)));
			System.out.println("  - Parsed : " + StanfordNlpWrapper.toTreeString(nlp.parse(words), "oneline"));
			int i = 0;
			for (Tree parse : nlp.parse(words, 10))
				System.out.printf("  - Parsed@%02d : %s\n", ++i, parse.toString());
		}
		assertTrue(StanfordNlpWrapper.detect(text).size() > 0);

		log.info("<PASSED> testMultipleParsing()");
	}

	/**
	 * Tests for pipeline functions
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testPiplineFunction() throws IOException {
		log.info("<START> testPiplineFunction()");

		String text = "Samsung Electronics is a South Korean multinational electronics company headquartered in Suwon, South Korea.";
		text += " It is the flagship subsidiary of the Samsung Group.";

		StanfordNlpWrapper nlp = new StanfordNlpWrapper(path);
		nlp.loadAll("tokenize, ssplit, pos, lemma, ner, regexner, parse, dcoref");
		assertTrue(nlp.annotator != null);

		Annotation annotation = nlp.annotate(text);
		System.out.println("-toXml--------------------------------------------------------------------------");
		System.out.println(nlp.toXml(annotation));
		System.out.println("-toPrettyStr--------------------------------------------------------------------");
		System.out.println(nlp.toPrettyStr(annotation));

		assertEquals(2, annotation.get(SentencesAnnotation.class).size());
		for (CoreMap sentence : annotation.get(SentencesAnnotation.class)) {
			System.out.println("-TextAnnotation-----------------------------------------------------------------");
			System.out.println(sentence.get(TextAnnotation.class));

			System.out.println("-toTokens-----------------------------------------------------------------------");
			System.out.println(JString.join("\n", StanfordNlpWrapper.toTokens(sentence, text)));

			System.out.println("-toPhrases-----------------------------------------------------------------------");
			System.out.println(JString.join("\n", StanfordNlpWrapper.toPhrases(sentence, text)));

			System.out.println("-TreeAnnotation-----------------------------------------------------------------");
			System.out.println(sentence.get(TreeAnnotation.class).pennString().trim());

			System.out.println("-BasicDependenciesAnnotation----------------------------------------------------");
			System.out.println(sentence.get(BasicDependenciesAnnotation.class).toString().trim());

			System.out.println("-CollapsedDependenciesAnnotation------------------------------------------------");
			System.out.println(sentence.get(CollapsedDependenciesAnnotation.class).toString().trim());

			System.out.println("-CollapsedCCProcessedDependenciesAnnotation-------------------------------------");
			System.out.println(sentence.get(CollapsedCCProcessedDependenciesAnnotation.class).toString().trim());
		}

		System.out.println("-toCoreferenceMap---------------------------------------------------------------");
		assertEquals(5, StanfordNlpWrapper.toCoreferenceMap(annotation).entrySet().size());
		for (Entry<Integer, List<CorefMention>> e : StanfordNlpWrapper.toCoreferenceMap(annotation).entrySet())
			for (CorefMention m : e.getValue())
				System.out.printf("%d\t%s\t%s\t%d\t%d\n", e.getKey(), m.mentionType, m.mentionSpan, m.sentNum, m.headIndex);

		log.info("<PASSED> testPiplineFunction()");
	}

	/**
	 * Tests for SUTime functions
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testSUTimeFunction() throws Exception {
		log.info("<START> testSUTimeFunction()");

		String text = "Last summer, they met every Tuesday afternoon, from 1 pm to 3 pm. I went to school yesterday. I will come back two weeks later.";
		String date = "2014-01-01";

		StanfordNlpWrapper nlp = new StanfordNlpWrapper(path);
		nlp.loadTimeAnnotator();
		assertTrue(nlp.normalizer != null);

		XmlReader reader = XmlReader.of(nlp.normalizeTime(text, date));
		assertEquals(6, reader.count("DOC/TEXT/TIMEX3").intValue());
		reader.findAll("DOC/TEXT/TIMEX3").map(JXml::toNodeString).forEach(System.out::println);

		log.info("<PASSED> testSUTimeFunction()");
	}

	/**
	 * Tests for utility functions
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testUtilityFunction() throws IOException {
		log.info("<START> testUtilityFunction()");

		String text = "Samsung Electronics is a South Korean multinational electronics company headquartered in Suwon, South Korea.";

		StanfordNlpWrapper nlp = new StanfordNlpWrapper(path);
		nlp.loadAll("tokenize, ssplit, pos, parse");
		assertTrue(nlp.annotator != null);

		Annotation annotation = nlp.annotate(text);
		CoreMap sentence = annotation.get(SentencesAnnotation.class).get(0);

		System.out.println("-toTokenStrings-----------------------------------------------------------------");
		List<String> toks = StanfordNlpWrapper.toTokenStrings(sentence);
		System.out.println(JString.join(" ", toks));

		System.out.println("-CollapsedCCProcessedDependenciesAnnotation-------------------------------------");
		System.out.println(sentence.get(CollapsedCCProcessedDependenciesAnnotation.class).toString().trim());

		System.out.println("[TEST] findHeadIndexBetween-----------------------------------------------------");
		int idx1 = toks.indexOf("Samsung");
		int idx2 = toks.indexOf("Electronics") + 1;
		int idx3 = toks.indexOf("company") + 1;

		Integer head1 = StanfordNlpWrapper.findHeadBetween(sentence, idx1, idx2);
		assertEquals("Electronics", toks.get(head1));
		System.out.printf("  <Head between [%d..%d)> = %d ==> %s\n", idx1, idx2, head1, toks.get(head1));

		Integer head2 = StanfordNlpWrapper.findHeadBetween(sentence, idx1, idx3);
		assertEquals("company", toks.get(head2));
		System.out.printf("  <Head between [%d..%d)> = %d ==> %s\n", idx1, idx3, head2, toks.get(head2));

		log.info("<PASSED> testUtilityFunction()");
	}
}
