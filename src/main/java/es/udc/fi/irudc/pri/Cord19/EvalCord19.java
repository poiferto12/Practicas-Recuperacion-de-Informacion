package es.udc.fi.irudc.pri.Cord19;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EvalCord19 {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Uso:");
            System.err.println("  mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.EvalCord19 \"-Dexec.args=<resultsFile.txt> <qrelsFile.txt> <outputCsv.csv>\"");
            System.err.println("");
            System.err.println("Parámetros:");
            System.err.println("  <resultsFile.txt>  Archivo generado por TopicSearcher");
            System.err.println("  <qrelsFile.txt>    Archivo qrels-covid_d5_j0.5-5.txt");
            System.err.println("  <outputCsv.csv>    Archivo CSV de salida con métricas por topic y promedio");
            System.exit(1);
        }

        String resultsPath = args[0];
        String qrelsPath = args[1];
        String outputPath = args[2];

        try {
            Map<String, Map<String, Integer>> qrels = P2UtilsCord19.parseQrels(qrelsPath);
            Map<String, List<P2UtilsCord19.SearchResult>> results = P2UtilsCord19.parseResults(resultsPath);

            double sumP10 = 0.0;
            double sumR10 = 0.0;
            double sumMAP10 = 0.0;
            double sumMAP100 = 0.0;
            double sumNDCG10 = 0.0;
            double sumNDCG100 = 0.0;
            int topicCount = 0;
            double sumRR = 0.0;

            List<String> csvLines = new ArrayList<>();
            csvLines.add("Topic,P@10,R@10,MAP@10,MAP@100,NDCG@10,NDCG@100,RR");

            for (String topic : qrels.keySet().stream().sorted((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b))).toList()) {
                Map<String, Integer> judgments = qrels.get(topic);
                List<P2UtilsCord19.SearchResult> topicResults = results.getOrDefault(topic, new ArrayList<>());

                P2UtilsCord19.Metrics m = P2UtilsCord19.computeMetricsForTopic(topicResults, judgments);

                sumP10 += m.pAt10;
                sumR10 += m.rAt10;
                sumMAP10 += m.mapAt10;
                sumMAP100 += m.mapAt100;
                sumNDCG10 += m.ndcgAt10;
                sumNDCG100 += m.ndcgAt100;
                topicCount++;
                sumRR += m.rr;

                csvLines.add(String.format(Locale.US,
                        "%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.3f",
                        topic, m.pAt10, m.rAt10, m.mapAt10, m.mapAt100, m.ndcgAt10, m.ndcgAt100, m.rr));
            }

            double avgP10 = topicCount > 0 ? sumP10 / topicCount : 0.0;
            double avgR10 = topicCount > 0 ? sumR10 / topicCount : 0.0;
            double avgMAP10 = topicCount > 0 ? sumMAP10 / topicCount : 0.0;
            double avgMAP100 = topicCount > 0 ? sumMAP100 / topicCount : 0.0;
            double avgNDCG10 = topicCount > 0 ? sumNDCG10 / topicCount : 0.0;
            double avgNDCG100 = topicCount > 0 ? sumNDCG100 / topicCount : 0.0;
            double avgRR = topicCount > 0 ? sumRR / topicCount : 0.0;

            csvLines.add(String.format(Locale.US,
                    "AVERAGE,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.3f",
                    avgP10, avgR10, avgMAP10, avgMAP100, avgNDCG10, avgNDCG100, avgRR));

            Files.write(Paths.get(outputPath), csvLines);

            System.out.println("Evaluación completada.");
            System.out.println("Topics evaluados: " + topicCount);
            System.out.printf(Locale.US, "P@10: %.6f%n", avgP10);
            System.out.printf(Locale.US, "R@10: %.6f%n", avgR10);
            System.out.printf(Locale.US, "MAP@10: %.6f%n", avgMAP10);
            System.out.printf(Locale.US, "MAP@100: %.6f%n", avgMAP100);
            System.out.printf(Locale.US, "NDCG@10: %.6f%n", avgNDCG10);
            System.out.printf(Locale.US, "NDCG@100: %.6f%n", avgNDCG100);
            System.out.printf(Locale.US, "RR: %.3f%n", avgRR);

            System.out.println("CSV generado: " + outputPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
