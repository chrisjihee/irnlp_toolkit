/**
 * Natural Language Processing package
 */
package kr.jihee.irnlp_toolkit.nlp;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.*;
import org.junit.*;

import kr.jihee.irnlp_toolkit.nlp.HannanumWrapper.TaggedMorp;
import kr.jihee.irnlp_toolkit.nlp.HannanumWrapper.TaggedWord;
import kr.jihee.text_toolkit.io.*;
import kr.jihee.text_toolkit.lang.JString;

/**
 * Unit test for functions using HanNanum
 * 
 * @author Jihee
 */
public class TestHannanum {
	static Logger log = LogManager.getLogger(Test.class);
	static String path = "resources/Hannanum_manual.xml";

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
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testBasicFunction() throws Exception {
		log.info("<START> testBasicFunction()");

		String text = "삼성전자는 대한민국에 본사를 둔 전자 제품을 생산하는 다국적 기업이다.";
		text += " 삼성전자는 대한민국에서 가장 큰 규모의 전자 기업이며, 삼성그룹을 대표하는 기업으로서 삼성그룹 안에서도 가장 규모가 크고 실적이 좋은 기업이다.";

		HannanumWrapper nlp = new HannanumWrapper(path);
		nlp.loadAll("ssplit, pos");
		assertTrue(nlp.detector != null);
		assertTrue(nlp.tagger != null);

		assertEquals(2, nlp.detect(text).size());
		for (String sent : nlp.detect(text)) {
			List<TaggedWord> taggedWords = nlp.tag(sent);
			List<TaggedMorp> taggedMorps = HannanumWrapper.toTaggedMorps(taggedWords);
			TaggedWord.DEFAULT_DELIMITER = "__";
			System.out.println("\n[Sentence] " + sent);
			System.out.println("  - Analyzed : " + JString.join(" ", taggedMorps));
			System.out.println("  - Tagged : " + JString.join(" ", taggedWords));
		}

		nlp.unload();

		log.info("<PASSED> testBasicFunction()");
	}
}
