/**
 * Natural Language Processing package
 */
package kr.jihee.irnlp_toolkit.nlp;

import java.io.*;
import java.util.*;
import java.util.stream.*;
import java.util.stream.Stream.*;
import java.util.zip.*;

import com.clearnlp.component.dep.*;
import com.clearnlp.component.pos.*;
import com.clearnlp.component.pred.*;
import com.clearnlp.component.role.*;
import com.clearnlp.component.srl.*;
import com.clearnlp.dependency.*;
import com.clearnlp.dependency.srl.*;
import com.clearnlp.nlp.*;
import com.clearnlp.reader.*;
import com.clearnlp.segmentation.*;
import com.clearnlp.tokenization.*;
import com.clearnlp.util.pair.*;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.trees.*;
import kr.jihee.text_toolkit.io.*;
import kr.jihee.text_toolkit.lang.*;
import kr.jihee.text_toolkit.lang.JList.*;
import kr.jihee.text_toolkit.lang.JObject.*;

/**
 * Wrapper of ClearNLP 2.0.2<br>
 * = URL : http://clearnlp.wikispaces.com/
 * 
 * @author Jihee
 */
public class ClearNlpWrapper {

	public Properties prop;
	public AbstractTokenizer tokenizer;
	public AbstractSegmenter detector;
	public AbstractPOSTagger tagger;
	public AbstractDEPParser parser;
	public AbstractPredicateIdentifier identifier;
	public AbstractRolesetClassifier classifier;
	public AbstractSRLabeler labeler;

	/**
	 * SRL label data
	 * 
	 * @author Jihee
	 */
	public static class SRLNode {

		public DEPNode node = null;
		public int depth = -1;

		public SRLNode(DEPNode node, int depth) {
			this.node = node;
			this.depth = depth;
		}

		public Map<String, Object> toMap() {
			return ClearNlpWrapper.toMap(node, depth);
		}

		public String toString() {
			return this.toMap().toString();
		}
	}

	public ClearNlpWrapper(String path) throws IOException {
		prop = new Properties();
		prop.loadFromXML(new FileInputStream(JFile.asFile(path)));
	}

	public ClearNlpWrapper loadTokenizer() throws IOException {
		tokenizer = NLPGetter.getTokenizer(AbstractReader.LANG_EN);
		return this;
	}

	public ClearNlpWrapper loadSentDetector() throws IOException {
		detector = NLPGetter.getSegmenter(AbstractReader.LANG_EN, tokenizer);
		return this;
	}

	public ClearNlpWrapper loadPosTagger() throws IOException {
		String model_path = prop.getProperty("pos.model");
		if (!model_path.toLowerCase().endsWith(".zip"))
			tagger = (AbstractPOSTagger) NLPGetter.getComponent(model_path, AbstractReader.LANG_EN, NLPMode.MODE_POS);
		else
			tagger = (AbstractPOSTagger) NLPGetter.getComponent(new ZipFile(model_path), AbstractReader.LANG_EN, NLPMode.MODE_POS);
		return this;
	}

	public ClearNlpWrapper loadDepParser() throws IOException {
		String model_path = prop.getProperty("dep.model");
		if (!model_path.toLowerCase().endsWith(".zip"))
			parser = (AbstractDEPParser) NLPGetter.getComponent(model_path, AbstractReader.LANG_EN, NLPMode.MODE_DEP);
		else
			parser = (AbstractDEPParser) NLPGetter.getComponent(new ZipFile(model_path), AbstractReader.LANG_EN, NLPMode.MODE_DEP);
		return this;
	}

	private ClearNlpWrapper loadPredIdentifier() throws IOException {
		String model_path = prop.getProperty("pred.model");
		if (!model_path.toLowerCase().endsWith(".zip"))
			identifier = (AbstractPredicateIdentifier) NLPGetter.getComponent(model_path, AbstractReader.LANG_EN, NLPMode.MODE_PRED);
		else
			identifier = (AbstractPredicateIdentifier) NLPGetter.getComponent(new ZipFile(model_path), AbstractReader.LANG_EN, NLPMode.MODE_PRED);
		return this;
	}

	private ClearNlpWrapper loadRoleClassifier() throws IOException {
		String model_path = prop.getProperty("role.model");
		if (!model_path.toLowerCase().endsWith(".zip"))
			classifier = (AbstractRolesetClassifier) NLPGetter.getComponent(model_path, AbstractReader.LANG_EN, NLPMode.MODE_ROLE);
		else
			classifier = (AbstractRolesetClassifier) NLPGetter.getComponent(new ZipFile(model_path), AbstractReader.LANG_EN, NLPMode.MODE_ROLE);
		return this;
	}

	public ClearNlpWrapper loadSrlLabeler() throws IOException {
		loadPredIdentifier();
		loadRoleClassifier();
		String model_path = prop.getProperty("srl.model");
		if (!model_path.toLowerCase().endsWith(".zip"))
			labeler = (AbstractSRLabeler) NLPGetter.getComponent(model_path, AbstractReader.LANG_EN, NLPMode.MODE_SRL);
		else
			labeler = (AbstractSRLabeler) NLPGetter.getComponent(new ZipFile(model_path), AbstractReader.LANG_EN, NLPMode.MODE_SRL);
		return this;
	}

	public ClearNlpWrapper loadAll() throws IOException {
		loadAll("tokenize, ssplit, pos, parse, srl");
		return this;
	}

	public ClearNlpWrapper loadAll(String annotator_spec) throws IOException {
		List<String> annotators = Arrays.asList(annotator_spec.toLowerCase().replaceAll("\\s", "").split(","));
		if (annotators.contains("tokenize"))
			loadTokenizer();
		if (annotators.contains("ssplit"))
			loadSentDetector();
		if (annotators.contains("pos"))
			loadPosTagger();
		if (annotators.contains("parse"))
			loadDepParser();
		if (annotators.contains("srl"))
			loadSrlLabeler();
		return this;
	}

	public List<List<String>> detect(String text) {
		return detector.getSentences(new BufferedReader(new StringReader(text)));
	}

	public List<String> tokenize(String sent) {
		return tokenizer.getTokens(sent);
	}

	public DEPTree tag(DEPTree units) {
		tagger.process(units);
		return units;
	}

	public DEPTree parse(DEPTree units) {
		parser.process(units);
		return units;
	}

	public List<DEPTree> parse(DEPTree units, int k) {
		List<ObjectDoublePair<DEPTree>> parsed_pairs = parser.getParsedTrees(units, true);
		List<DEPTree> parsed_trees = new ArrayList<DEPTree>();
		for (int i = 0; i < Math.min(k, parsed_pairs.size()); i++)
			parsed_trees.add((DEPTree) parsed_pairs.get(i).o);
		return parsed_trees;
	}

	public DEPTree label(DEPTree units) {
		identifier.process(units);
		classifier.process(units);
		labeler.process(units);
		return units;
	}

	public DEPTree annotate(String sent) {
		if (tokenizer == null)
			throw new NullPointerException("Tokenizer is not loaded yet.");
		List<String> tokens = tokenize(sent);
		return annotate(tokens);
	}

	public DEPTree annotate(List<String> tokens) {
		DEPTree units = NLPGetter.toDEPTree(tokens);
		return annotate(units);
	}

	public DEPTree annotate(DEPTree units) {
		if (tagger != null)
			tag(units);
		if (parser != null)
			parse(units);
		if (identifier != null && classifier != null && labeler != null)
			label(units);
		return units;
	}

	/**
	 * Transform a DEPNode instance into a Word instance
	 * 
	 * @param unit
	 * @return
	 */
	public static Word toWord(DEPNode unit) {
		return new Word(unit.form);
	}

	/**
	 * Transform a DEPNode instance into a TaggedWord instance
	 * 
	 * @param unit
	 * @return
	 */
	public static TaggedWord toTaggedWord(DEPNode unit) {
		return new TaggedWord(unit.form, unit.pos);
	}

	/**
	 * Transform a DEPNode instance into a CoreLabel instance
	 * 
	 * @param unit
	 * @return
	 */
	public static IndexedWord toIndexedWord(DEPNode unit) {
		IndexedWord new_unit = new IndexedWord();
		new_unit.setIndex(unit.id);
		new_unit.setValue(unit.form);
		new_unit.setWord(unit.form);
		new_unit.setTag(unit.pos);
		new_unit.setLemma(unit.lemma);
		new_unit.set(TreeCoreAnnotations.HeadTagAnnotation.class, new TreeGraphNode(new StringLabel(unit.pos)));
		return new_unit;
	}

	/**
	 * Transform a DEPNode instance into a TypedDependency instance
	 * 
	 * @param unit
	 * @return
	 */
	public static TypedDependency toTypedDependency(DEPNode unit) {
		if (!unit.hasHead())
			return null;
		GrammaticalRelation reln = StanfordNlpWrapper.getGrammaticalRelation(unit.getLabel());
		IndexedWord gov = toIndexedWord(unit.getHead());
		IndexedWord dep = toIndexedWord(unit);
		return new TypedDependency(reln, gov, dep);
	}

	/**
	 * Transform a SRLArc instance and a DEPNode instance into a TypedDependency instance
	 * 
	 * @param sarc
	 * @param unit
	 * @return
	 */
	public static TypedDependency toTypedDependency(SRLArc sarc, DEPNode unit) {
		if (!unit.hasHead())
			return null;
		String label = sarc.getLabel();
		if (!sarc.getFunctionTag().isEmpty())
			label += "_" + sarc.getFunctionTag();
		GrammaticalRelation reln = StanfordNlpWrapper.getGrammaticalRelation(label);
		IndexedWord gov = toIndexedWord(sarc.getNode());
		IndexedWord dep = toIndexedWord(unit);
		return new TypedDependency(reln, gov, dep);
	}

	/**
	 * Transform a DEPNode instance into a Map instance
	 * 
	 * @param unit
	 * @param text
	 * @return
	 */
	public static JMap toBriefMap(DEPNode unit, Object text) {
		JMap map = JMap.empty();
		map.put("idx", unit.id);
		map.put("lemma", unit.lemma);
		map.put("pos", unit.pos);
		map.put("text", text);
		return map;
	}

	/**
	 * Transform a DEPNode instance into a Map instance
	 * 
	 * @param unit
	 * @return
	 */
	public static JMap toBriefMap(DEPNode unit) {
		return toBriefMap(unit, StrCaster.or(unit.getFeat(DEPLib.FEAT_PB), unit.form));
	}

	/**
	 * Transform a DEPNode instance into a Map instance
	 * 
	 * @param unit
	 * @return
	 */
	public static JMap toMap(DEPNode unit) {
		return toMap(unit, 0);
	}

	/**
	 * Transform a DEPNode instance into a Map instance
	 * 
	 * @param unit
	 * @param depth
	 * @return
	 */
	public static JMap toMap(DEPNode unit, int depth) {
		JMap map = JMap.empty();
		map.put("id", unit.id);
		map.put("form", unit.form);
		map.put("lemma", unit.lemma);
		map.put("pos", unit.pos);
		map.put("depth", depth);
		if (unit.hasHead()) {
			map.put("drel", unit.getLabel());
			map.put("governor", unit.getHead().id);
			map.put("pb", unit.getFeat(DEPLib.FEAT_PB));
			if (unit.getSHead(unit.getHead()) != null) {
				map.put("srel", unit.getSHead(unit.getHead()).getLabel());
				map.put("sfunc", unit.getSHead(unit.getHead()).getFunctionTag());
			}
		}
		return map;
	}

	public static DEPTree toList(List<String> toks) {
		return NLPGetter.toDEPTree(toks);
	}

	/**
	 * Transform a tokinized DEPTree instance into a list of Word instances
	 * 
	 * @param units
	 * @return
	 */
	public static List<Word> toWords(DEPTree units) {
		ArrayList<Word> new_units = new ArrayList<Word>();
		for (int i = 1; i < units.size(); i++)
			new_units.add(toWord(units.get(i)));
		return new_units;
	}

	/**
	 * Transform a POS tagged DEPTree instance into a list of TaggedWord instances
	 * 
	 * @param units
	 * @return
	 */
	public static List<TaggedWord> toTaggedWords(DEPTree units) {
		ArrayList<TaggedWord> new_units = new ArrayList<TaggedWord>();
		for (int i = 1; i < units.size(); i++)
			new_units.add(toTaggedWord(units.get(i)));
		return new_units;
	}

	/**
	 * Transform a dependency parsed DEPTree instance into a list of TypedDependency instances
	 * 
	 * @param units
	 * @return
	 */
	public static List<TypedDependency> toTypedDependencies(DEPTree units) {
		ArrayList<TypedDependency> new_units = new ArrayList<TypedDependency>();
		for (int i = 1; i < units.size(); i++)
			new_units.add(toTypedDependency(units.get(i)));
		return new_units;
	}

	/**
	 * Transform a semantic role labelled DEPTree instance into a list of TypedDependency instances
	 * 
	 * @param units
	 * @return
	 */
	public static List<TypedDependency> toSemanticRoleLabels(DEPTree units) {
		ArrayList<TypedDependency> new_units = new ArrayList<TypedDependency>();
		for (int i = 1; i < units.size(); i++)
			for (SRLArc sarc : units.get(i).getSHeads())
				new_units.add(toTypedDependency(sarc, units.get(i)));
		return new_units;
	}

	/**
	 * Extract all predicates from a SRL labelled sentence
	 * 
	 * @param units
	 * @return
	 */
	public static List<DEPNode> getPredicates(DEPTree units) {
		List<DEPNode> preds = new ArrayList<DEPNode>();
		for (DEPNode unit : units)
			if (unit.getFeat(DEPLib.FEAT_PB) != null)
				preds.add(unit);
		return preds;
	}

	/**
	 * Extract all predicates from a SRL labelled sentence
	 * 
	 * @param units
	 * @param labels
	 * @return
	 */
	public static List<DEPNode> getPredicates(DEPTree units, List<String> labels) {
		List<DEPNode> preds = new ArrayList<DEPNode>();
		for (DEPNode unit : units)
			if (unit.getFeat(DEPLib.FEAT_PB) != null && labels.contains(unit.getLabel()))
				preds.add(unit);
		return preds;
	}

	/**
	 * Extract all the semantic roles of a given predicate
	 * 
	 * @param units
	 * @param pred
	 * @param skipSecondary
	 * @return
	 */
	public static List<SRLArc> getSemanticRoles(DEPTree units, DEPNode pred, boolean skipSecondary) {
		if (pred.getFeat(DEPLib.FEAT_PB) == null)
			return null;
		List<SRLArc> arcs = new ArrayList<SRLArc>();
		for (DEPNode unit : units) {
			SRLArc sarc = unit.getSHead(pred);
			if (sarc != null)
				if (!skipSecondary || !sarc.getLabel().startsWith(SRLLib.PREFIX_CONCATENATION) && !sarc.getLabel().startsWith(SRLLib.PREFIX_REFERENT))
					arcs.add(new SRLArc(unit, sarc.getLabel(), sarc.getFunctionTag()));
		}
		return arcs;
	}

	/**
	 * Extract all the semantic roles of a given predicate
	 * 
	 * @param units
	 * @param pred
	 * @return
	 */
	public static List<SRLArc> getSemanticRoles(DEPTree units, DEPNode pred) {
		return getSemanticRoles(units, pred, true);
	}

	final static List<String> nonValidLabels = Arrays.asList("punct", "cc");

	public static List<JMap> getPhrases(DEPNode root, boolean skipPrep) {
		GList<DEPNode> nodes = GList.of(root);
		Builder<GList<DEPNode>> phrases = Stream.builder();
		buildPhrases(phrases, nodes, root, skipPrep, true);

		List<JMap> maps = GList.empty();
		phrases.build().forEach(phrase -> {
			DEPNode head = phrase.get(0);
			phrase.sort((p1, p2) -> p1.id - p2.id);
			while (nonValidLabels.contains(phrase.peekLast().getLabel()))
				phrase.removeLast();
			String text = JString.join(" ", phrase.parallelStream().map(node -> node.form));
			maps.add(toBriefMap(head, text));
		});
		return maps;
	}

	final static List<String> nonNominalLabels = Arrays.asList("advmod", "rcmod");
	final static List<String> prepositionalLabels = Arrays.asList("prep");
	final static List<String> conjunctiveLabels = Arrays.asList("conj");

	private static void buildPhrases(Builder<GList<DEPNode>> builder, GList<DEPNode> nodes, DEPNode head, boolean skipPrep, boolean toAdd) {
		if (toAdd)
			builder.add(nodes);
		for (DEPNode node : head.getDependentNodeList()) {
			String label = node.getLabel();
			if (nonNominalLabels.contains(label))
				continue;
			else if (skipPrep && prepositionalLabels.contains(label))
				continue;
			else if (conjunctiveLabels.contains(label))
				buildPhrases(builder, GList.of(node), node, skipPrep, true);
			else
				buildPhrases(builder, nodes.append(node), node, skipPrep, false);
		}
	}

	/**
	 * Extract all the dependent units from a DEP parsed sentence
	 * 
	 * @param unit
	 * @return
	 */
	public static List<SRLNode> getDependents(DEPNode unit) {
		return getDependents(unit, false);
	}

	/**
	 * Extract all the dependent units from a DEP parsed sentence
	 * 
	 * @param unit
	 * @param inclSelf
	 * @return
	 */
	public static List<SRLNode> getDependents(DEPNode unit, boolean inclSelf) {
		return getDependents(unit, inclSelf, 1);
	}

	/**
	 * Extract all the dependent units from a DEP parsed sentence
	 * 
	 * @param unit
	 * @param inclSelf
	 * @param limit
	 * @return
	 */
	public static List<SRLNode> getDependents(DEPNode unit, boolean inclSelf, int limit) {
		return getDependents(unit, inclSelf, limit, 0);
	}

	/**
	 * Extract all the dependent units from a DEP parsed sentence
	 * 
	 * @param unit
	 * @param inclSelf
	 * @param limit
	 * @param depth
	 * @return
	 */
	public static List<SRLNode> getDependents(DEPNode unit, boolean inclSelf, int limit, int depth) {
		List<SRLNode> deps = new ArrayList<SRLNode>();
		if (inclSelf)
			deps.add(new SRLNode(unit, depth));

		if (depth + 1 <= limit)
			for (DEPNode dep : unit.getDependentNodeList()) {
				deps.add(new SRLNode(dep, depth + 1));
				deps.addAll(getDependents(dep, false, limit, depth + 1));
			}

		return deps;
	}

	/**
	 * Extract all the dependent units from a DEP parsed sentence
	 * 
	 * @param unit
	 * @return
	 */
	public static List<Map<String, Object>> getDependents2(DEPNode unit) {
		return getDependents2(unit, false);
	}

	/**
	 * Extract all the dependent units from a DEP parsed sentence
	 * 
	 * @param unit
	 * @param inclSelf
	 * @return
	 */
	public static List<Map<String, Object>> getDependents2(DEPNode unit, boolean inclSelf) {
		return getDependents2(unit, inclSelf, 1);
	}

	/**
	 * Extract all the dependent units from a DEP parsed sentence
	 * 
	 * @param unit
	 * @param inclSelf
	 * @param limit
	 * @return
	 */
	public static List<Map<String, Object>> getDependents2(DEPNode unit, boolean inclSelf, int limit) {
		return getDependents2(unit, inclSelf, limit, 0);
	}

	/**
	 * Extract all the dependent units from a DEP parsed sentence
	 * 
	 * @param unit
	 * @param inclSelf
	 * @param limit
	 * @param depth
	 * @return
	 */
	public static List<Map<String, Object>> getDependents2(DEPNode unit, boolean inclSelf, int limit, int depth) {
		List<Map<String, Object>> dep_infos = new ArrayList<Map<String, Object>>();
		if (inclSelf)
			dep_infos.add(toMap(unit, depth));

		if (depth + 1 <= limit)
			for (DEPNode dep : unit.getDependentNodeList()) {
				dep_infos.add(toMap(dep, depth + 1));
				dep_infos.addAll(getDependents2(dep, false, limit, depth + 1));
			}

		return dep_infos;
	}

	/**
	 * Replace old POS tags into new POS tags
	 * 
	 * @param units old POS tags
	 * @param new_units new POS tags
	 */
	public static void replacePos(DEPTree units, List<TaggedWord> new_units) {
		for (int i = 1; i < units.size(); i++)
			units.get(i).pos = new_units.get(i - 1).tag();
	}

	/**
	 * Replace old dependency arcs into new dependency arcs
	 * 
	 * @param units
	 * @param new_units
	 */
	public static void replaceDep(DEPTree units, List<TypedDependency> new_units) {
		for (TypedDependency new_unit : new_units) {
			DEPNode gov = units.get(new_unit.gov().index());
			DEPNode dep = units.get(new_unit.dep().index());
			String reln = new_unit.reln().toString();
			dep.setHead(new DEPArc(gov, reln));
		}
	}
}
