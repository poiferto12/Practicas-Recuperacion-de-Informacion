package es.udc.fi.irudc.pri.Cord19;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

public class BestValuesCord19 {

    public static class BestResult {
        public final String field;
        public final double lambda;
        public final double value;

        public BestResult(String field, double lambda, double value) {
            this.field = field;
            this.lambda = lambda;
            this.value = value;
        }
    }

    private static final float[] LAMBDAS = {
            0.01f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f,
            0.6f, 0.7f, 0.8f, 0.9f, 1.0f
    };

    private static final String[] FIELDS = {"title", "abstract", "full_text"};

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Uso:");
            System.err.println("  mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.BestValuesCord19 \"-Dexec.args=<indexPath> <topicsFile> <qrelsFile> <outputDir>\"");
            System.err.println("");
            System.err.println("Parámetros:");
            System.err.println("  <indexPath>   Ruta al índice Lucene");
            System.err.println("  <topicsFile>  Ruta al archivo topics-rnd5.xml");
            System.err.println("  <qrelsFile>   Ruta al archivo qrels-covid_d5_j0.5-5.txt");
            System.err.println("  <outputDir>   Carpeta donde guardar lo generado");
            System.exit(1);
        }

        String indexPath = args[0];
        String topicsPath = args[1];
        String qrelsPath = args[2];
        String outputDir = args[3];

        try {
            File outDir = new File(outputDir);
            if (!outDir.exists()) {
                outDir.mkdirs();
            }

            List<P2UtilsCord19.TrecTopic> topics = P2UtilsCord19.parseTopics(topicsPath);
            Map<String, Map<String, Integer>> qrels = P2UtilsCord19.parseQrels(qrelsPath);

            FSDirectory dir = FSDirectory.open(Paths.get(indexPath));
            DirectoryReader reader = DirectoryReader.open(dir);

            Map<String, BestResult> bestMap100 = new HashMap<>();
            Map<String, BestResult> bestNdcg100 = new HashMap<>();
            List<String> summaryLines = new ArrayList<>();
            summaryLines.add("Field,Lambda,MAP@100,NDCG@100");

            for (String field : FIELDS) {
                double bestMapValue = -1.0;
                double bestNdcgValue = -1.0;
                double bestMapLambda = -1.0;
                double bestNdcgLambda = -1.0;

                for (float lambda : LAMBDAS) {
                    String lambdaStr = String.format(Locale.US, "%.2f", lambda);
                    String runPath = outputDir + File.separator + "run_" + field + "_lambda" + lambdaStr + ".txt";
                    String runTag = "LMJelinekMercer_lambda" + lambdaStr + "_" + field;

                    runSearch(reader, topics, runPath, field, lambda, runTag);

                    Map<String, List<P2UtilsCord19.SearchResult>> results = P2UtilsCord19.parseResults(runPath);
                    double map100 = averageMetric(results, qrels, "MAP@100");
                    double ndcg100 = averageMetric(results, qrels, "NDCG@100");

                    summaryLines.add(String.format(Locale.US,
                            "%s,%.2f,%.6f,%.6f", field, lambda, map100, ndcg100));

                    System.out.printf(Locale.US,
                            "Campo=%s Lambda=%.2f MAP@100=%.6f NDCG@100=%.6f%n",
                            field, lambda, map100, ndcg100);

                    if (map100 > bestMapValue) {
                        bestMapValue = map100;
                        bestMapLambda = lambda;
                    }
                    if (ndcg100 > bestNdcgValue) {
                        bestNdcgValue = ndcg100;
                        bestNdcgLambda = lambda;
                    }
                }

                bestMap100.put(field, new BestResult(field, bestMapLambda, bestMapValue));
                bestNdcg100.put(field, new BestResult(field, bestNdcgLambda, bestNdcgValue));
            }

            summaryLines.add("");
            summaryLines.add("Best MAP@100");
            summaryLines.add("Field,BestLambda,MAP@100");
            for (String field : FIELDS) {
                BestResult br = bestMap100.get(field);
                summaryLines.add(String.format(Locale.US, "%s,%.2f,%.6f", br.field, br.lambda, br.value));
            }

            summaryLines.add("");
            summaryLines.add("Best NDCG@100");
            summaryLines.add("Field,BestLambda,NDCG@100");
            for (String field : FIELDS) {
                BestResult br = bestNdcg100.get(field);
                summaryLines.add(String.format(Locale.US, "%s,%.2f,%.6f", br.field, br.lambda, br.value));
            }

            String summaryPath = outputDir + File.separator + "best_values_summary.csv";
            Files.write(Paths.get(summaryPath), summaryLines);

            reader.close();
            dir.close();

            System.out.println("Resumen generado: " + summaryPath);
            System.out.println("Copia los resultados Best MAP@100 y Best NDCG@100 al README.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runSearch(DirectoryReader reader,
                                  List<P2UtilsCord19.TrecTopic> topics,
                                  String runPath,
                                  String field,
                                  float lambda,
                                  String runTag) throws IOException {
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new LMJelinekMercerSimilarity(lambda));
        QueryParser parser = new QueryParser(field, new StandardAnalyzer());

        try (PrintWriter writer = new PrintWriter(new FileWriter(runPath))) {
            writer.printf(Locale.US,
                    "# Campo: %s | Rango: 1-50 | Lambda: %.2f | Modelo: LMJelinekMercer%n",
                    field, lambda);

            for (P2UtilsCord19.TrecTopic topic : topics) {
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
                    }
                } catch (ParseException e) {
                    System.err.println("Error parseando topic " + topic.getId() + ": " + topic.getQuery());
                }
            }
        }
    }

    private static double averageMetric(Map<String, List<P2UtilsCord19.SearchResult>> results,
                                        Map<String, Map<String, Integer>> qrels,
                                        String metricName) {
        double sum = 0.0;
        int count = 0;

        for (String topic : qrels.keySet()) {
            List<P2UtilsCord19.SearchResult> topicResults = results.getOrDefault(topic, new ArrayList<>());
            P2UtilsCord19.Metrics metrics = P2UtilsCord19.computeMetricsForTopic(topicResults, qrels.get(topic));
            sum += metrics.getByName(metricName);
            count++;
        }

        return count > 0 ? sum / count : 0.0;
    }
}
