package es.udc.fi.irudc.pri.Cord19;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.FSDirectory;

public class TopicSearcher {

    public static void main(String[] args) {
        if (args.length < 6) {
            System.err.println("Uso:");
            System.err.println("  mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.TopicSearcher \"-Dexec.args=<indexPath> <topicsFile> <runOutput.txt> <lambda> <field> <range>\"");
            System.err.println("");
            System.err.println("Parámetros:");
            System.err.println("  <indexPath>      Ruta al índice Lucene");
            System.err.println("  <topicsFile>     Ruta al archivo topics-rnd5.xml");
            System.err.println("  <runOutput.txt>  Archivo de salida en formato TREC");
            System.err.println("  <lambda>         Valor de lambda ");
            System.err.println("  <field>          Campo de búsqueda: title, abstract, full_text o full-text");
            System.err.println("  <range>          Rango de topics: 1-50, 5-5, 10-29, etc.");
            System.exit(1);
        }

        String indexPath = args[0];
        String topicsPath = args[1];
        String runOutputPath = args[2];
        float lambda = Float.parseFloat(args[3]);
        String field = P2UtilsCord19.normalizeField(args[4]);
        String range = args[5];
        int[] rangeLimits = P2UtilsCord19.parseRange(range);
        int startTopic = rangeLimits[0];
        int endTopic = rangeLimits[1];

        String lambdaStr = String.format(Locale.US, "%.2f", lambda);
        String runTag = "LMJelinekMercer_lambda" + lambdaStr + "_" + field;

        try {
            List<P2UtilsCord19.TrecTopic> topics = P2UtilsCord19.parseTopics(topicsPath);
            System.out.println("Topics cargados: " + topics.size());

            FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
            DirectoryReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new LMJelinekMercerSimilarity(lambda));

            StandardAnalyzer analyzer = new StandardAnalyzer();
            QueryParser parser = new QueryParser(field, analyzer);

            PrintWriter writer = new PrintWriter(new FileWriter(runOutputPath));
            writer.printf(Locale.US,
                    "# Campo: %s | Rango: %s | Lambda: %.2f | Modelo: LMJelinekMercer%n",
                    field, range, lambda);

            int processedTopics = 0;
            int totalResults = 0;

            for (P2UtilsCord19.TrecTopic topic : topics) {
                if (!P2UtilsCord19.isTopicInRange(topic.getId(), startTopic, endTopic)) {
                    continue;
                }

                try {
                    Query query = parser.parse(QueryParser.escape(topic.getQuery()));
                    TopDocs topDocs = searcher.search(query, P2UtilsCord19.RUN_CUTOFF);
                    ScoreDoc[] hits = topDocs.scoreDocs;

                    for (int i = 0; i < hits.length && i < P2UtilsCord19.RUN_CUTOFF; i++) {
                        Document doc = searcher.doc(hits[i].doc);
                        String cordUid = doc.get("cord_uid");

                        if (cordUid == null || cordUid.isBlank()) {
                            continue;
                        }

                        writer.printf(Locale.US,
                                "%s Q0 %s %d %.6f %s%n",
                                topic.getId(), cordUid, i + 1, hits[i].score, runTag);
                        totalResults++;
                    }
                    processedTopics++;
                } catch (ParseException e) {
                    System.err.println("Error parseando topic " + topic.getId() + ": " + topic.getQuery());
                    e.printStackTrace();
                }
            }

            writer.close();
            reader.close();
            dir.close();

            System.out.println("Búsqueda completada.");
            System.out.println("Topics procesados: " + processedTopics);
            System.out.println("Resultados escritos: " + totalResults);
            System.out.println("Archivo generado: " + runOutputPath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
