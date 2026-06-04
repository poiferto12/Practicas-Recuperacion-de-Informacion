package es.udc.fi.irudc.pri.Cord19;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class StatsCord19 {

    static class TermStat {
        String term;
        long tf;
        long df;
        double idflog10;
        double tfidf;

        TermStat(String term, long tf, long df, double idflog10, double tfidf) {
            this.term = term;
            this.tf = tf;
            this.df = df;
            this.idflog10 = idflog10;
            this.tfidf = tfidf;
        }
    }

    static class CollectionTermStat {
        String term;
        long df;
        long totalTermFreq;

        CollectionTermStat(String term, long df, long totalTermFreq) {
            this.term = term;
            this.df = df;
            this.totalTermFreq = totalTermFreq;
        }
    }

    public static void main(String[] args) {
        if(args.length < 2){
            System.out.println("Uso: java StatsCord19 <indexPath> [cord_uid] <field>");
            System.out.println("Ejemplo: (estadísticas de documento): mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.StatsCord19 \"-Dexec.args=index-cord19 0c1ud3y5 title\"");
            System.out.println("Ejemplo: (estadísticas de colección): mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.StatsCord19 \"-Dexec.args=index-cord19 title\"");
            System.out.println();
            System.out.println("Parámetros:");
            System.out.println("  <indexPath>  - Ruta al directorio del índice");
            System.out.println("  <cord_uid>   - Identificador único del documento");
            System.out.println("  <field>      - Campo a analizar (title, abstract, authors, full_text, journal, etc.)");
            return;
        }

        String indexDir = args[0];

        try{
            FSDirectory dir = FSDirectory.open(Paths.get(indexDir));
            IndexReader reader = DirectoryReader.open(dir);

            if(args.length == 2){
                String field = args[1];
                performTask5(reader, field);
            }else if (args.length == 3) {
                String cordUid = args[1];
                String field = args[2];
                performTask4(reader, cordUid, field);
            }else {
                System.err.println("Error: Número de argumentos inválido");
            }

            reader.close();
            dir.close();

        }catch (IOException e) {
            System.err.println("Error: No se puede abrir el índice: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static void performTask4(IndexReader reader, String cordUid, String field) {
        try{
            IndexSearcher searcher = new IndexSearcher(reader);
            TermQuery query = new TermQuery(new Term("cord_uid", cordUid));

            int docId;
            Document doc;
            var topDocs = searcher.search(query, 1);

            if(topDocs.totalHits.value > 0) {
                docId = topDocs.scoreDocs[0].doc;
                doc = searcher.storedFields().document(docId);
            }else{
                System.err.println("Error: No se encontró ningún documento con cord_uid: " + cordUid);
                return;
            }

            // Mostrar informacion basica de documento
            String title = doc.get("title");
            System.out.println("-----------------------------------------------------");
            System.out.println("cord_uid: " + cordUid);
            System.out.println("Título:  " + title);
            System.out.println("Campo:   " + field);
            String fieldContent = doc.get(field);
            System.out.println("Contenido del campo '" + field + "': ");
            System.out.println(fieldContent);
            System.out.println("-----------------------------------------------------");

            Terms terms = reader.termVectors().get(docId, field);
            if(terms == null) {
                System.err.println("Error: No hay term vector disponible para el campo '" + field + "'.");
                return;
            }

            int totalDocs = reader.numDocs();
            long totalTerms = terms.size();
            System.out.println("Número total de términos: " + totalTerms);

            List<TermStat> termStats = new ArrayList<>();
            TermsEnum termsEnum = terms.iterator();
            BytesRef termBytes;

            while ((termBytes = termsEnum.next()) != null){
                String termText = termBytes.utf8ToString();
                long tf = termsEnum.totalTermFreq();
                long df = reader.docFreq(new Term(field, termText));
                double idf = Math.log10((totalDocs + 1.0) / (df + 1.0));
                double tfidf = tf * idf;
                termStats.add(new TermStat(termText, tf, df, idf, tfidf));
            }
            termStats.sort(Comparator.comparingDouble((TermStat ts) -> ts.tfidf).reversed());

            System.out.println("\nTop 10 términos ordenados por tf x idflog10:");
            System.out.printf("%-20s %-10s %-10s %-15s%n", "Término", "tf", "idf", "tf x idf");
            System.out.println("-----------------------------------------------------");
            int limit = Math.min(10, termStats.size());
            for(int i = 0; i < limit; i++) {
                TermStat ts = termStats.get(i);
                System.out.printf("%-20s %-10d %-10.4f %-15.4f%n", ts.term, ts.tf, ts.idflog10, ts.tfidf);
            }
            termStats.sort(Comparator.comparingLong((TermStat ts) -> ts.tf).reversed());

            System.out.println("\nTop 10 términos ordenados por TF:");
            System.out.printf("%-20s %-10s %-10s %-15s%n", "Término", "tf", "idf", "tf x idf");
            System.out.println("-----------------------------------------------------");
            for (int i = 0; i < limit; i++){
                TermStat ts = termStats.get(i);
                System.out.printf("%-20s %-10d %-10.4f %-15.4f%n", ts.term, ts.tf, ts.idflog10, ts.tfidf);
            }
            termStats.sort(Comparator.comparingDouble((TermStat ts) -> ts.idflog10).reversed());

            System.out.println("\nTop 10 términos ordenados por IDFlog10:");
            System.out.printf("%-20s %-10s %-10s %-15s%n", "Término", "tf", "idf", "tf x idf");
            System.out.println("-----------------------------------------------------");
            for (int i = 0; i < limit; i++){
                TermStat ts = termStats.get(i);
                System.out.printf("%-20s %-10d %-10.4f %-15.4f%n", ts.term, ts.tf, ts.idflog10, ts.tfidf);
            }

        }catch (IOException e){
            System.err.println("Error: No se puede procesar la tarea 4: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static void performTask5(IndexReader reader, String field) {
        try{
            System.out.println("-----------------------------------------------------");
            System.out.println("Campo: " + field);

            Terms terms = org.apache.lucene.index.MultiTerms.getTerms(reader, field);
            if (terms == null) {
                System.err.println("Error: Campo '" + field + "' no encontrado en el índice");
                return;
            }


            List<CollectionTermStat> termStatsList = new ArrayList<>();
            TermsEnum termsEnum = terms.iterator();
            BytesRef term;

            while((term = termsEnum.next()) != null) {
                String termStr = term.utf8ToString();
                long df = termsEnum.docFreq();
                long totalTermFreq = termsEnum.totalTermFreq();

                termStatsList.add(new CollectionTermStat(termStr, df, totalTermFreq));
            }

            long totalTerms = termStatsList.size();
            System.out.println("Total de términos únicos: " + totalTerms);

            List<CollectionTermStat> sortedByDF = new ArrayList<>(termStatsList);
            sortedByDF.sort(Comparator.comparingLong((CollectionTermStat ts) -> ts.df));

            System.out.println("\n========== 10 TÉRMINOS MÁS ESPECÍFICOS (DF más bajo) ==========");
            System.out.printf("%-20s %-10s %-15s%n", "Término", "DF", "Total Term Freq");
            System.out.println("-----------------------------------------------------");
            for(int i = 0; i < Math.min(10, sortedByDF.size()); i++){
                CollectionTermStat ts = sortedByDF.get(i);
                System.out.printf("%-20s %-10d %-15d%n", ts.term, ts.df, ts.totalTermFreq);
            }

            sortedByDF.sort(Comparator.comparingLong((CollectionTermStat ts) -> ts.df).reversed());

            System.out.println("\n========== 10 TÉRMINOS MENOS ESPECÍFICOS (DF más alto) ==========");
            System.out.printf("%-20s %-10s %-15s%n", "Término", "DF", "Total Term Freq");
            System.out.println("-----------------------------------------------------");
            for(int i = 0; i < Math.min(10, sortedByDF.size()); i++){
                CollectionTermStat ts = sortedByDF.get(i);
                System.out.printf("%-20s %-10d %-15d%n", ts.term, ts.df, ts.totalTermFreq);
            }
            List<CollectionTermStat> sortedByFreq = new ArrayList<>(termStatsList);
            sortedByFreq.sort(Comparator.comparingLong((CollectionTermStat ts) -> ts.totalTermFreq));

            System.out.println("\n========== 10 TÉRMINOS MENOS FRECUENTES  ==========");
            System.out.printf("%-20s %-10s %-15s%n", "Término", "DF", "Total Term Freq");
            System.out.println("-----------------------------------------------------");
            for(int i = 0; i < Math.min(10, sortedByFreq.size()); i++){
                CollectionTermStat ts = sortedByFreq.get(i);
                System.out.printf("%-20s %-10d %-15d%n", ts.term, ts.df, ts.totalTermFreq);
            }

            sortedByFreq.sort(Comparator.comparingLong((CollectionTermStat ts) -> ts.totalTermFreq).reversed());

            System.out.println("\n========== 10 TÉRMINOS MÁS FRECUENTES ==========");
            System.out.printf("%-20s %-10s %-15s%n", "Término", "DF", "Total Term Freq");
            System.out.println("-----------------------------------------------------");
            for(int i = 0; i < Math.min(10, sortedByFreq.size()); i++){
                CollectionTermStat ts = sortedByFreq.get(i);
                System.out.printf("%-20s %-10d %-15d%n", ts.term, ts.df, ts.totalTermFreq);
            }
            System.out.println("-----------------------------------------------------");

        }catch (IOException e) {
            System.err.println("Error: No se puede procesar la tarea 5: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}

