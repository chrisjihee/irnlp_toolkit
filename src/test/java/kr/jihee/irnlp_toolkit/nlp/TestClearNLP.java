/**
 * Natural Language Processing package
 */
package kr.jihee.irnlp_toolkit.nlp;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import kr.jihee.text_toolkit.io.*;
import kr.jihee.text_toolkit.lang.*;

import org.apache.logging.log4j.*;
import org.junit.*;

import com.clearnlp.component.dep.*;
import com.clearnlp.component.pos.*;
import com.clearnlp.dependency.*;
import com.clearnlp.dependency.srl.*;
import com.clearnlp.nlp.*;
import com.clearnlp.reader.*;
import com.clearnlp.tokenization.*;
import com.clearnlp.util.pair.*;

import edu.stanford.nlp.semgraph.*;

/**
 * Unit test for functions using ClearNLP
 * 
 * @author Jihee
 */
public class TestClearNLP {
	static Logger log = LogManager.getLogger(Test.class);
	static String path = "resources/ClearNLP_default.xml";

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

		ClearNlpWrapper nlp = new ClearNlpWrapper(path);
		nlp.loadAll("tokenize, ssplit, pos, parse");
		assertTrue(nlp.tokenizer != null);
		assertTrue(nlp.detector != null);
		assertTrue(nlp.tagger != null);
		assertTrue(nlp.parser != null);

		for (List<String> words : nlp.detect(text)) {
			SemanticGraph sgraph = StanfordNlpWrapper.toSemanticGraph(ClearNlpWrapper.toTypedDependencies(nlp.annotate(words)), false);
			System.out.println("\n[Sentence] " + JString.join(" ", words));
			System.out.println("  - Tagged : " + JString.join(" ", ClearNlpWrapper.toTaggedWords(nlp.annotate(words))));
			System.out.println("  - Parsed : " + JString.join("; ", ClearNlpWrapper.toTypedDependencies(nlp.annotate(words))));
			System.out.println("  - Formed : \n" + JString.trimAndIndent(sgraph.toFormattedString(), 4));
			System.out.println(JString.trimAndIndent(sgraph.toString(), 4));
			assertEquals(words.size(), ClearNlpWrapper.toTaggedWords(nlp.annotate(words)).size());
			assertEquals(words.size(), ClearNlpWrapper.toTypedDependencies(nlp.annotate(words)).size());
			assertEquals(words.size(), sgraph.toString().split("\n").length);
		}
		assertTrue(nlp.detect(text).size() > 0);

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

		String modelType = "general-en";
		String language = AbstractReader.LANG_EN;

		AbstractTokenizer tokenizer = NLPGetter.getTokenizer(language);
		AbstractPOSTagger tagger = (AbstractPOSTagger) NLPGetter.getComponent(modelType, language, NLPMode.MODE_POS);
		AbstractDEPParser parser = (AbstractDEPParser) NLPGetter.getComponent(modelType, language, NLPMode.MODE_DEP);

		String sentence = "Power on your Kindle.";
		DEPTree tree = NLPGetter.toDEPTree(tokenizer.getTokens(sentence));
		boolean uniqueOnly = false;
		tagger.process(tree);
		List<ObjectDoublePair<DEPTree>> trees = parser.getParsedTrees(tree, uniqueOnly);
		for (ObjectDoublePair<DEPTree> p : trees) {
			DEPTree tree2 = (DEPTree) p.o;
			double score = p.d;
			System.out.println("----------------------------");
			System.out.println(score);

			System.out.println("----------------------------");
			System.out.println(tree2.toStringPOS());

			System.out.println("----------------------------");
			System.out.println(tree2.toStringCoNLL());
		}
		System.out.println("==========================");

		log.info("<PASSED> testMultipleParsing()");
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

		String text = "Samsung Electronics is a South Korean multinational electronics company in Suwon, South Korea.";

		ClearNlpWrapper nlp = new ClearNlpWrapper(path);
		nlp.loadAll("tokenize, ssplit, pos, parse, srl");
		assertTrue(nlp.tokenizer != null);
		assertTrue(nlp.detector != null);
		assertTrue(nlp.tagger != null);
		assertTrue(nlp.parser != null);
		assertTrue(nlp.labeler != null);

		List<String> toks = nlp.detect(text).get(0);
		DEPTree units = NLPGetter.toDEPTree(toks);
		units = nlp.tag(units);
		units = nlp.parse(units);
		units = nlp.label(units);
		System.out.println("\n[Sentence] " + JString.join(" ", toks));
		System.out.println("  - SRL : ");
		System.out.println(JString.indent(units.toStringSRL(), 4));
		assertEquals(toks.size(), units.toStringSRL().split("\n").length);

		if (ClearNlpWrapper.toSemanticRoleLabels(units).size() > 0) {
			System.out.println("  - SRs : " + JString.join("; ", ClearNlpWrapper.toSemanticRoleLabels(units)));
			assertTrue(ClearNlpWrapper.toSemanticRoleLabels(units).size() > 0);

			System.out.println("-labeled SemanticGraph----------------------------------------------------------");
			SemanticGraph sgraph = StanfordNlpWrapper.toSemanticGraph(ClearNlpWrapper.toSemanticRoleLabels(units));
			System.out.println(JString.trimAndIndent(sgraph.toString(), 2));
			assertTrue(sgraph.edgeCount() > 0);
			assertTrue(sgraph.size() > 0);
			assertEquals("is", sgraph.getFirstRoot().word());

			System.out.println("-labeled Arguments--------------------------------------------------------------");
			StringBuffer sb = new StringBuffer();
			for (DEPNode pred : ClearNlpWrapper.getPredicates(units)) {
				sb.append("->(Predicate) " + pred.getFeat(DEPLib.FEAT_PB) + "\n");
				for (SRLArc sr : ClearNlpWrapper.getSemanticRoles(units, pred, true))
					sb.append(String.format("  ->(Argument) %s (%s_%s)\n", sr.getNode().form, sr.getLabel(), sr.getFunctionTag()));
			}
			System.out.println(JString.trimAndIndent(sb.toString(), 2));
			assertEquals("is", ClearNlpWrapper.getPredicates(units).get(0).form);
		}

		log.info("<PASSED> testAdvancedFunction()");
	}
}
