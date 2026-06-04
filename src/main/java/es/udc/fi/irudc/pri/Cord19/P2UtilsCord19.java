package es.udc.fi.irudc.pri.Cord19;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;


public final class P2UtilsCord19 {

    private P2UtilsCord19() {
        // Utility class
    }

    public static final int MAX_TOPIC = 50;
    public static final int RUN_CUTOFF = 100;

    public static class TrecTopic {
        private final String id;
        private final String query;

        public TrecTopic(String id, String query) {
            this.id = id;
            this.query = query;
        }

        public String getId() {
            return id;
        }

        public String getQuery() {
            return query;
        }
    }

    public static class SearchResult {
        public final String topic;
        public final String docId;
        public final int rank;
        public final double score;
        public final String runTag;

        public SearchResult(String topic, String docId, int rank, double score, String runTag) {
            this.topic = topic;
            this.docId = docId;
            this.rank = rank;
            this.score = score;
            this.runTag = runTag;
        }
    }

    public static class Metrics {
        public double pAt10;
        public double rAt10;
        public double mapAt10;
        public double mapAt100;
        public double ndcgAt10;
        public double ndcgAt100;
        public double rr;

        public double getByName(String metricName) {
            return switch (metricName) {
                case "P@10" -> pAt10;
                case "R@10" -> rAt10;
                case "MAP@10" -> mapAt10;
                case "MAP@100" -> mapAt100;
                case "NDCG@10" -> ndcgAt10;
                case "NDCG@100" -> ndcgAt100;
                case "RR" -> rr;
                default -> throw new IllegalArgumentException("Métrica no válida: " + metricName);
            };
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Topics(
            @JacksonXmlElementWrapper(useWrapping = false)
            @JacksonXmlProperty(localName = "topic")
            List<Topic> topics
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Topic(
                String number,
                String query,
                String question,
                String narrative
        ) {}
    }

    public static List<TrecTopic> parseTopics(String topicsFilePath) throws IOException {
        XmlMapper mapper = XmlMapper.builder().build();
        Topics topics = mapper.readValue(new File(topicsFilePath), Topics.class);

        List<TrecTopic> topicsList = new ArrayList<>();
        if (topics.topics() != null) {
            for (Topics.Topic topic : topics.topics()) {
                topicsList.add(new TrecTopic(topic.number(), topic.query()));
            }
        }
        return topicsList;
    }

    public static String normalizeField(String field) {
        if (field == null) {
            throw new IllegalArgumentException("El campo no puede ser null");
        }
        if (field.equals("full-text")) {
            return "full_text";
        }
        if (field.equals("title") || field.equals("abstract") || field.equals("full_text")) {
            return field;
        }
        throw new IllegalArgumentException("Campo no válido: " + field + ". Usa title, abstract o full_text/full-text");
    }

    public static int[] parseRange(String range) {
        String[] parts = range.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Rango inválido. Ejemplo: 1-50");
        }

        int start = Integer.parseInt(parts[0]);
        int end = Integer.parseInt(parts[1]);

        if (start < 1 || end > MAX_TOPIC || start > end) {
            throw new IllegalArgumentException("Rango inválido. Debe estar entre 1 y 50, por ejemplo 1-50");
        }
        return new int[]{start, end};
    }

    public static boolean isTopicInRange(String topicId, int start, int end) {
        int topicNumber = Integer.parseInt(topicId);
        return topicNumber >= start && topicNumber <= end;
    }

    public static Map<String, Map<String, Integer>> parseQrels(String qrelsPath) throws IOException {
        Map<String, Map<String, Integer>> qrels = new HashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(qrelsPath));

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 4) {
                continue;
            }

            String topic = parts[0];
            String docId = parts[2];
            int judgment = Integer.parseInt(parts[3]);

            qrels.computeIfAbsent(topic, k -> new HashMap<>()).put(docId, judgment);
        }

        return qrels;
    }

    public static Map<String, List<SearchResult>> parseResults(String resultsPath) throws IOException {
        Map<String, List<SearchResult>> results = new HashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(resultsPath));

        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 6) {
                continue;
            }

            String topic = parts[0];
            String docId = parts[2];
            int rank = Integer.parseInt(parts[3]);
            double score = Double.parseDouble(parts[4].replace(',', '.'));
            String runTag = parts[5];

            SearchResult sr = new SearchResult(topic, docId, rank, score, runTag);
            results.computeIfAbsent(topic, k -> new ArrayList<>()).add(sr);
        }

        results.values().forEach(list -> list.sort(Comparator.comparingInt(r -> r.rank)));
        return results;
    }

    public static double calculatePrecision(List<SearchResult> results, Map<String, Integer> judgments, int cutoff) {
        int relevant = 0;
        int limit = Math.min(cutoff, results.size());

        for (int i = 0; i < limit; i++) {
            if (judgments.getOrDefault(results.get(i).docId, 0) > 0) {
                relevant++;
            }
        }


        return ((double) relevant) / cutoff;
    }

    public static double calculateRecall(List<SearchResult> results, Map<String, Integer> judgments, int cutoff) {
        int totalRelevant = (int) judgments.values().stream().filter(j -> j > 0).count();
        if (totalRelevant == 0) {
            return 0.0;
        }

        int relevant = 0;
        int limit = Math.min(cutoff, results.size());
        for (int i = 0; i < limit; i++) {
            if (judgments.getOrDefault(results.get(i).docId, 0) > 0) {
                relevant++;
            }
        }

        return ((double) relevant) / totalRelevant;
    }


    public static double calculateAP(List<SearchResult> results, Map<String, Integer> judgments, int cutoff) {
        int totalRelevant = (int) judgments.values().stream().filter(j -> j > 0).count();
        int denominator = Math.min(totalRelevant, cutoff);
        if (denominator == 0) {
            return 0.0;
        }

        double sumPrecision = 0.0;
        int relevantSeen = 0;
        int limit = Math.min(cutoff, results.size());

        for (int i = 0; i < limit; i++) {
            if (judgments.getOrDefault(results.get(i).docId, 0) > 0) {
                relevantSeen++;
                sumPrecision += ((double) relevantSeen) / (i + 1);
            }
        }

        return sumPrecision / denominator;
    }


    public static double calculateDCG(List<SearchResult> results, Map<String, Integer> judgments, int cutoff) {
        double dcg = 0.0;
        int limit = Math.min(cutoff, results.size());

        for (int i = 0; i < limit; i++) {
            int rel = judgments.getOrDefault(results.get(i).docId, 0);
            if (rel > 0) {
                double discount = Math.log(2.0) / Math.log(i + 2.0);
                dcg += rel * discount;
            }
        }

        return dcg;
    }

    public static double calculateIDCG(Map<String, Integer> judgments, int cutoff) {
        List<Integer> relevanceScores = new ArrayList<>(judgments.values());
        relevanceScores.sort((a, b) -> Integer.compare(b, a));

        double idcg = 0.0;
        int limit = Math.min(cutoff, relevanceScores.size());
        for (int i = 0; i < limit; i++) {
            int rel = relevanceScores.get(i);
            if (rel > 0) {
                double discount = Math.log(2.0) / Math.log(i + 2.0);
                idcg += rel * discount;
            }
        }

        return idcg;
    }

    public static double calculateNDCG(List<SearchResult> results, Map<String, Integer> judgments, int cutoff) {
        double dcg = calculateDCG(results, judgments, cutoff);
        double idcg = calculateIDCG(judgments, cutoff);
        return idcg == 0.0 ? 0.0 : dcg / idcg;
    }

    public static double calculateRR(List<SearchResult> results, Map<String, Integer> judgments){
        int limit = results.size();
        for (int i = 0; i < limit; i++){
            if (judgments.getOrDefault(results.get(i).docId, 0) > 0) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    public static Metrics computeMetricsForTopic(List<SearchResult> results, Map<String, Integer> judgments) {
        Metrics metrics = new Metrics();
        metrics.pAt10 = calculatePrecision(results, judgments, 10);
        metrics.rAt10 = calculateRecall(results, judgments, 10);
        metrics.mapAt10 = calculateAP(results, judgments, 10);
        metrics.mapAt100 = calculateAP(results, judgments, 100);
        metrics.ndcgAt10 = calculateNDCG(results, judgments, 10);
        metrics.ndcgAt100 = calculateNDCG(results, judgments, 100);
        metrics.rr = calculateRR(results, judgments);
        return metrics;
    }

    public static boolean isValidMetric(String metricName) {
        return metricName.equals("P@10") || metricName.equals("R@10")
                || metricName.equals("MAP@10") || metricName.equals("MAP@100")
                || metricName.equals("NDCG@10") || metricName.equals("NDCG@100")
                || metricName.equals("RR");
    }

    // Kendall tau-b sin dependencia
    public static double kendallTauB(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Los vectores deben tener la misma longitud");
        }
        if (x.length < 2) {
            return 0.0;
        }

        long concordant = 0;
        long discordant = 0;
        long tiesX = 0;
        long tiesY = 0;

        for (int i = 0; i < x.length - 1; i++) {
            for (int j = i + 1; j < x.length; j++) {
                int cmpX = Double.compare(x[i], x[j]);
                int cmpY = Double.compare(y[i], y[j]);

                if (cmpX == 0 && cmpY == 0) {
                    // Empate en ambos: no cuenta para tiesX ni tiesY en tau-b.
                    continue;
                } else if (cmpX == 0) {
                    tiesX++;
                } else if (cmpY == 0) {
                    tiesY++;
                } else if (cmpX == cmpY) {
                    concordant++;
                } else {
                    discordant++;
                }
            }
        }

        double denominator = Math.sqrt((concordant + discordant + tiesX) * (double) (concordant + discordant + tiesY));
        if (denominator == 0.0) {
            return 0.0;
        }
        return (concordant - discordant) / denominator;
    }
}
