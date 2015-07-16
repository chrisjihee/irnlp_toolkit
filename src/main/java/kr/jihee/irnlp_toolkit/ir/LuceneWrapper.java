/**
 * Information Retrieval package
 */
package kr.jihee.irnlp_toolkit.ir;

import static org.apache.lucene.index.IndexWriterConfig.OpenMode.*;
import static org.apache.lucene.util.Version.*;

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

/**
 * Wrapper of Lucene 4.8.1<br>
 * - URL : http://lucene.apache.org/core/
 * 
 * @author Jihee
 */
public class LuceneWrapper {
	public static double DEFAULT_MEM = 256;
	public FSDirectory index_dir;

	/**
	 * SearchedEntry
	 * 
	 * @author Jihee
	 */
	public static class SearchedEntry {
		public Document doc;
		public float score;

		public SearchedEntry(Document doc, float score) {
			this.doc = doc;
			this.score = score;
		}
	}

	public LuceneWrapper(String index_dir) throws IOException {
		this.index_dir = FSDirectory.open(new File(index_dir));
	}

	public IndexWriter createIndexWriter(boolean create) throws IOException {
		IndexWriterConfig iwc = new IndexWriterConfig(LUCENE_48, new StandardAnalyzer(LUCENE_48));
		iwc.setOpenMode(create ? CREATE : CREATE_OR_APPEND);
		iwc.setRAMBufferSizeMB(DEFAULT_MEM);
		return new IndexWriter(index_dir, iwc);
	}

	public IndexSearcher createIndexSearcher() throws IOException {
		return new IndexSearcher(DirectoryReader.open(index_dir));
	}

	public QueryParser createQueryParser(String field) {
		return new QueryParser(LUCENE_48, field, new StandardAnalyzer(LUCENE_48));
	}

	public List<SearchedEntry> search(IndexSearcher searcher, Query query, int k) throws IOException {
		ArrayList<SearchedEntry> searched_entries = new ArrayList<>();
		for (ScoreDoc searched_doc : searcher.search(query, k).scoreDocs) {
			Document doc = searcher.doc(searched_doc.doc);
			float score = searched_doc.score;
			searched_entries.add(new SearchedEntry(doc, score));
		}
		return searched_entries;
	}
}
