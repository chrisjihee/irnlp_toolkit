/**
 * Natural Language Processing package
 */
package kr.jihee.irnlp_toolkit.nlp;

import static org.junit.Assert.*;

import java.io.*;

import kr.jihee.text_toolkit.io.*;
import kr.jihee.text_toolkit.lang.*;

import org.apache.logging.log4j.*;
import org.junit.*;

/**
 * Unit test for functions using OpenNLP
 * 
 * @author Jihee
 */
public class TestOpenNLP {
	static Logger log = LogManager.getLogger(Test.class);
	static String path = "resources/OpenNLP.xml";

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
	 */
	@Test
	@Ignore
	public void testBasicFunction() throws IOException {
		log.info("<START> testBasicFunction()");

		String text = "Samsung Electronics is a South Korean multinational electronics company headquartered in Suwon, South Korea.";
		text += " It is the flagship subsidiary of the Samsung Group.";

		OpenNlpWrapper nlp = new OpenNlpWrapper(path);
		nlp.loadAll("ssplit, tokenize, pos, chunk, parse");
		assertTrue(nlp.detector != null);
		assertTrue(nlp.tokenizer != null);
		assertTrue(nlp.tagger != null);
		assertTrue(nlp.chunker != null);
		assertTrue(nlp.parser != null);

		for (String sent : nlp.detect(text)) {
			String[] toks = nlp.tokenize(sent);
			String[] tags = nlp.tag(toks);
			String[] chunks = nlp.chunk(toks, tags);
			System.out.println("\n[Sentence] " + sent);
			System.out.println("  - Chunked : " + OpenNlpWrapper.toChunkString(toks, tags, chunks));
			System.out.println("  - Parsed : " + OpenNlpWrapper.toTreeString(nlp.parse(sent)));
		}
		assertEquals(2, nlp.detect(text).length);

		log.info("<PASSED> testBasicFunction()");
	}

	/**
	 * Tests for advanced functions
	 * 
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testAdvancedFunction() throws IOException {
		log.info("<START> testAdvancedFunction()");

		String text = "Samsung Electronics is a South Korean multinational electronics company headquartered in Suwon, South Korea.";
		text += " It is the flagship subsidiary of the Samsung Group.";

		OpenNlpWrapper nlp = new OpenNlpWrapper(path);
		nlp.loadAll("ssplit, tokenize, ner");
		assertTrue(nlp.detector != null);
		assertTrue(nlp.tokenizer != null);
		assertTrue(nlp.recognizers != null);

		assertEquals(2, nlp.detect(text).length);
		for (String sent : nlp.detect(text)) {
			String[] toks = nlp.tokenize(sent);
			System.out.println("\n[Sentence] " + sent);
			System.out.println("  - Recognized : " + JString.join(", ", nlp.recognize(toks)));
		}

		log.info("<PASSED> testAdvancedFunction()");
	}
}
