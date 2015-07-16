/**
 * Machine Learning package
 */
package kr.jihee.irnlp_toolkit.ml;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import kr.jihee.irnlp_toolkit.ml.MalletWrapper.CRFResult;
import kr.jihee.irnlp_toolkit.ml.MalletWrapper.LDAResult;
import kr.jihee.irnlp_toolkit.ml.MalletWrapper.MalletCRFWrapper;
import kr.jihee.irnlp_toolkit.ml.MalletWrapper.MalletLDAWrapper;
import kr.jihee.text_toolkit.lang.*;

import org.apache.logging.log4j.*;
import org.junit.*;

import cc.mallet.types.*;

/**
 * Unit test for functions using Mallet
 * 
 * @author Jihee
 */
public class TestMallet {
	static Logger log = LogManager.getLogger(Test.class);

	/**
	 * Tests for basic CRF functions
	 */
	@Test
	@Ignore
	public void testBasicCRF() {
		log.info("<START> testBasicCRF()");

		ArrayList<String> items = new ArrayList<String>();
		items.add("John/NNP/NP loves/VBZ/O ice/NN/NP cream/NN/NP cake/NN/NP ././O");
		items.add("But/CC/O he/PRP/NP does/VBZ/O not/RB/O love/VB/O orange/JJ/NP juice/NN/NP ././O");
		items.add("You/PRP/NP do/VBP/O not/RB/O eat/VB/O apple/NN/NP ././O");
		items.add("But/CC/O Mary/NNP/NP loves/VBZ/O it/PRP/NP ././O");
		for (int i = 0; i < items.size(); i++)
			items.set(i, items.get(i).replace(" ", "\n").replace("/", "  "));

		String[] data1 = items.subList(0, 2).toArray(new String[0]);
		assertEquals(2, data1.length);
		String[] data2 = items.subList(2, items.size()).toArray(new String[0]);
		assertEquals(2, data2.length);

		MalletCRFWrapper.DEFAULT_TARGET_PROCESSING = true;
		MalletCRFWrapper crf = new MalletCRFWrapper();
		crf.setTrainData(data1);
		assertEquals(2, crf.train_data.size());
		crf.train(500);
		assertEquals(true, crf.model != null);

		crf.setTestData(data2);
		assertEquals(2, crf.test_data.size());
		List<CRFResult> results = crf.test(1);
		assertEquals(2, results.size());
		for (CRFResult result : results) {
			assertEquals(1, result.outputs.size());
			System.out.println(result.input.toString().trim());
			System.out.println(String.join(" ", result.outputs.get(0)));
			System.out.println();
		}

		log.info("<PASSED> testBasicCRF()");
	}

	/**
	 * Tests for basic LDA functions
	 */
	@Test
	@Ignore
	public void testBasicLDA() throws IOException {
		log.info("<START> testBasicLDA()");

		System.out.println("\n----- testMalletLDAWrapper() ------------------------------");
		String kr_def = "Korea called Hanguk in South Korea and Chosŏn in North Korea, is an East Asian territory that is divided into two distinct sovereign states, North Korea and South Korea. Located on the Korean Peninsula, Korea is bordered by China to the northwest and Russia to the northeast. It is separated from Japan to the east by the Korea Strait and the Sea of Japan (East Sea). The adoption of the Chinese writing system in the 2nd century BC and the introduction of Buddhism in the 4th century AD had profound effects on the Three Kingdoms of Korea, which was first united during the Silla (57 BC – AD 935) under the King Munmu. The united Silla fell to Goryeo in 935 at the end of the Later Three Kingdoms. Goryeo was a highly cultured state and created the Jikji in the 14th century. The invasions by the Mongolians in the 13th century, however, greatly weakened the nation, which was forced to become a tributary state. After the Mongol Empire's collapse, severe political strife followed. The Ming-allied Joseon emerged supreme in 1388.";
		String jp_def = "Japan is an island nation in East Asia. Located in the Pacific Ocean, it lies to the east of the Sea of Japan, China, North Korea, South Korea and Russia, stretching from the Sea of Okhotsk in the north to the East China Sea and Taiwan in the south. The characters that make up Japan's name mean sun-origin, which is why Japan is often referred to as the Land of the Rising Sun.";
		String cn_def = "China is a sovereign state located in East Asia. It is the world's most populous country, with a population of over 1.35 billion. The PRC is a single-party state governed by the Communist Party, with its seat of government in the capital city of Beijing.[15] It exercises jurisdiction over 22 provinces, five autonomous regions, four direct-controlled municipalities (Beijing, Tianjin, Shanghai, and Chongqing), and two mostly self-governing special administrative regions (Hong Kong and Macau). The PRC also claims Taiwan – which is controlled by the Republic of China (ROC), a separate political entity – as its 23rd province, a claim which is controversial due to the complex political status of Taiwan.[16]";
		String it_def = "Italy is a unitary parliamentary republic in Southern Europe. To the north, Italy borders France, Switzerland, Austria, and Slovenia, and is approximately delimited by the Alpine watershed, enclosing the Po Valley and the Venetian Plain. To the south, it consists of the entirety of the Italian Peninsula and the two biggest Mediterranean islands of Sicily and Sardinia.";
		String fr_def = "France is a sovereign country in Western Europe that includes several overseas regions and territories.[note 13] Metropolitan France extends from the Mediterranean Sea to the English Channel and the North Sea, and from the Rhine to the Atlantic Ocean. It is one of only three countries (with Morocco and Spain) to have both Atlantic and Mediterranean coastlines. Due to its shape, it is often referred to in French as l’Hexagone.";
		String en_def = "England is a country that is part of the United Kingdom.[2][3][4] It shares land borders with Scotland to the north and Wales to the west. The Irish Sea lies north west of England, whilst the Celtic Sea lies to the south west. The North Sea to the east and the English Channel to the south separate it from continental Europe. Most of England comprises the central and southern part of the island of Great Britain which lies in the North Atlantic. The country also includes over 100 smaller islands such as the Isles of Scilly, and the Isle of Wight.";
		LinkedHashMap<String, String> items = new LinkedHashMap<String, String>();
		items.put("kr", kr_def);
		items.put("jp", jp_def);
		items.put("cn", cn_def);
		items.put("it", it_def);
		items.put("fr", fr_def);
		items.put("en", en_def);

		MalletLDAWrapper lda = new MalletLDAWrapper();
		lda.setInputData(items);
		assertEquals(6, lda.data.size());

		List<LDAResult> results = lda.cluster(2, 1000);
		assertEquals(6, results.size());
		System.out.println("-Topic Distribution-------------------------------------------------------------");
		for (LDAResult result : results) {
			List<String> dists = new ArrayList<String>();
			for (IDSorter scoredTopic : result.outputs)
				dists.add(String.format("%d(%.4f)", scoredTopic.getID(), scoredTopic.getWeight()));
			System.out.println(JString.join("\t", result.id, result.name, JString.join(", ", dists)));
		}

		assertEquals(2, lda.clusteredWords.size());
		System.out.println("-Word Distribution--------------------------------------------------------------");
		for (Integer id : lda.clusteredWords.keySet())
			System.out.printf("%d\t%s\n", id, String.join(" ", lda.clusteredWords.get(id)));

		log.info("<PASSED> testBasicLDA()");
	}
}
