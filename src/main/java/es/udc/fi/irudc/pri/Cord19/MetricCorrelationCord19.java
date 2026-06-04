package es.udc.fi.irudc.pri.Cord19;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MetricCorrelationCord19 {

    private static final String[] METRIC_NAMES = {
            "P@10", "R@10", "MAP@10", "MAP@100", "NDCG@10", "NDCG@100", "RR"
    };

    public static class QueryMetrics {
        public final String queryId;
        public final P2UtilsCord19.Metrics metrics;

        public QueryMetrics(String queryId, P2UtilsCord19.Metrics metrics) {
            this.queryId = queryId;
            this.metrics = metrics;
        }

        public double averageDifficultyValue() {
            return (metrics.pAt10 + metrics.rAt10 + metrics.mapAt10
                    + metrics.mapAt100 + metrics.ndcgAt10 + metrics.ndcgAt100 + metrics.rr) / 7.0;
        }
    }

    private static double[] getMetricVectorValue(QueryMetrics qm) {
        return new double[]{
                qm.metrics.pAt10,
                qm.metrics.rAt10,
                qm.metrics.mapAt10,
                qm.metrics.mapAt100,
                qm.metrics.ndcgAt10,
                qm.metrics.ndcgAt100,
                qm.metrics.rr
        };
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Uso:");
            System.err.println("  mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.MetricCorrelationCord19 \"-Dexec.args=<resultsFile.txt> <qrelsFile.txt> <range> <outputFile.txt>\"");
            System.err.println("");
            System.err.println("Parámetros:");
            System.err.println("  <resultsFile.txt>  Archivo run generado por TopicSearcher");
            System.err.println("  <qrelsFile.txt>    Archivo qrels-covid_d5_j0.5-5.txt");
            System.err.println("  <range>            Rango de queries: 1-50, 5-5, 10-29, etc.");
            System.err.println("  <outputFile.txt>   Archivo donde guardar ranking de dificultad y matriz Kendall tau-b");
            System.exit(1);
        }

        String resultsPath = args[0];
        String qrelsPath = args[1];
        String range = args[2];
        Path outputPath = Paths.get(args[3]);

        try {
            int[] rangeLimits = P2UtilsCord19.parseRange(range);
            int start = rangeLimits[0];
            int end = rangeLimits[1];

            Map<String, Map<String, Integer>> qrels = P2UtilsCord19.parseQrels(resultsPath.equals(qrelsPath) ? qrelsPath : qrelsPath);
            Map<String, List<P2UtilsCord19.SearchResult>> results = P2UtilsCord19.parseResults(resultsPath);

            List<QueryMetrics> queryMetricsList = new ArrayList<>();

            List<String> sortedQueryIds = new ArrayList<>(qrels.keySet());
            sortedQueryIds.sort(Comparator.comparingInt(Integer::parseInt));

            for (String queryId : sortedQueryIds) {
                if (!P2UtilsCord19.isTopicInRange(queryId, start, end)) {
                    continue;
                }

                List<P2UtilsCord19.SearchResult> queryResults =
                        results.getOrDefault(queryId, new ArrayList<>());

                if (queryResults.isEmpty()) {
                    System.err.println("Aviso: no hay resultados para la query " + queryId);
                    continue;
                }

                P2UtilsCord19.Metrics metrics =
                        P2UtilsCord19.computeMetricsForTopic(queryResults, qrels.get(queryId));

                queryMetricsList.add(new QueryMetrics(queryId, metrics));
            }

            if (queryMetricsList.isEmpty()) {
                System.err.println("No se han encontrado queries evaluables en el rango " + range);
                System.exit(1);
            }

            queryMetricsList.sort(Comparator.comparingDouble(QueryMetrics::averageDifficultyValue));

            double[][] metricVectors = new double[METRIC_NAMES.length][queryMetricsList.size()];

            for (int i = 0; i < queryMetricsList.size(); i++) {
                double[] values = getMetricVectorValue(queryMetricsList.get(i));

                for (int j = 0; j < METRIC_NAMES.length; j++) {
                    metricVectors[j][i] = values[j];
                }
            }

            double[][] correlations = new double[METRIC_NAMES.length][METRIC_NAMES.length];

            for (int i = 0; i < METRIC_NAMES.length; i++) {
                for (int j = 0; j < METRIC_NAMES.length; j++) {
                    if (i == j) {
                        correlations[i][j] = 1.0;
                    } else {
                        correlations[i][j] =
                                P2UtilsCord19.kendallTauB(metricVectors[i], metricVectors[j]);
                    }
                }
            }

            List<String> lines = new ArrayList<>();

            lines.add("MetricCorrelationCord19");
            lines.add("Results file: " + resultsPath);
            lines.add("Qrels file: " + qrelsPath);
            lines.add("Rango: " + range);
            lines.add("Queries evaluadas: " + queryMetricsList.size());
            lines.add("");
            lines.add("Queries ordenadas de más difícil a más fácil por promedio de métricas");
            lines.add("Query,DifficultyAverage,P@10,R@10,MAP@10,MAP@100,NDCG@10,NDCG@100");

            for (QueryMetrics qm : queryMetricsList) {
                lines.add(String.format(Locale.US,
                        "%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.3f",
                        qm.queryId,
                        qm.averageDifficultyValue(),
                        qm.metrics.pAt10,
                        qm.metrics.rAt10,
                        qm.metrics.mapAt10,
                        qm.metrics.mapAt100,
                        qm.metrics.ndcgAt10,
                        qm.metrics.ndcgAt100,
                        qm.metrics.rr));
            }

            lines.add("");
            lines.add("Matriz Kendall tau-b entre métricas");

            StringBuilder header = new StringBuilder("Metric");
            for (String metricName : METRIC_NAMES) {
                header.append(',').append(metricName);
            }
            lines.add(header.toString());

            for (int i = 0; i < METRIC_NAMES.length; i++) {
                StringBuilder row = new StringBuilder(METRIC_NAMES[i]);

                for (int j = 0; j < METRIC_NAMES.length; j++) {
                    row.append(',').append(String.format(Locale.US, "%.6f", correlations[i][j]));
                }

                lines.add(row.toString());
            }

            Path parent = outputPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(outputPath, lines);

            for (String line : lines) {
                System.out.println(line);
            }

            System.out.println();
            System.out.println("Salida guardada en: " + outputPath.toAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}