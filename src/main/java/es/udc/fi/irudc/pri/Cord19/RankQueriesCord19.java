package es.udc.fi.irudc.pri.Cord19;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RankQueriesCord19 {

    public static class QueryMetric {
        public final String queryId;
        public final double metricValue;

        public QueryMetric(String queryId, double metricValue) {
            this.queryId = queryId;
            this.metricValue = metricValue;
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Uso:");
            System.err.println("  mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.RankQueriesCord19 \"-Dexec.args=<resultsFile.txt> <qrelsFile.txt> <range> <metric>\"");
            System.err.println("");
            System.err.println("Parámetros:");
            System.err.println("  <resultsFile.txt>  Archivo run generado por TopicSearcher");
            System.err.println("  <qrelsFile.txt>    Archivo qrels-covid_d5_j0.5-5.txt");
            System.err.println("  <range>            Rango de queries: 1-50, 5-5, 10-29, etc.");
            System.err.println("  <metric>           Métrica: P@10, R@10, MAP@10, MAP@100, NDCG@10 o NDCG@100");
            System.exit(1);
        }

        String resultsPath = args[0];
        String qrelsPath = args[1];
        String range = args[2];
        String metricName = args[3];

        if (!P2UtilsCord19.isValidMetric(metricName)) {
            System.err.println("ERROR: Métrica inválida: " + metricName);
            System.exit(1);
        }

        int[] rangeLimits = P2UtilsCord19.parseRange(range);
        int start = rangeLimits[0];
        int end = rangeLimits[1];

        try {
            Map<String, Map<String, Integer>> qrels = P2UtilsCord19.parseQrels(qrelsPath);
            Map<String, List<P2UtilsCord19.SearchResult>> results = P2UtilsCord19.parseResults(resultsPath);

            List<QueryMetric> queryMetrics = new ArrayList<>();

            for (String queryId : qrels.keySet()) {
                if (!P2UtilsCord19.isTopicInRange(queryId, start, end)) {
                    continue;
                }

                List<P2UtilsCord19.SearchResult> queryResults = results.getOrDefault(queryId, new ArrayList<>());
                P2UtilsCord19.Metrics metrics = P2UtilsCord19.computeMetricsForTopic(queryResults, qrels.get(queryId));
                queryMetrics.add(new QueryMetric(queryId, metrics.getByName(metricName)));
            }

            // Menor valor de métrica = consulta más difícil.
            queryMetrics.sort(Comparator.comparingDouble(q -> q.metricValue));

            System.out.println("Queries ordenadas de más difícil a más fácil según " + metricName);
            System.out.printf("%-10s %-15s%n", "Query", metricName);
            System.out.println("-".repeat(30));

            for (QueryMetric qm : queryMetrics) {
                System.out.printf(Locale.US, "%-10s %-15.6f%n", qm.queryId, qm.metricValue);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
