package es.udc.fi.irudc.pri.Cord19;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class SearchCord19 {
    public static void main(String[] args) {
        if (args.length < 3 || args.length > 4){
            System.out.println("Uso: java SearchCord19 <indexPath> <field> <queryText> [topN]");
            System.out.println("Ejemplo: mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.SearchCord19 \"-Dexec.args=index-cord19 title coronavirus 15\"");
            System.out.println();
            System.out.println("Parámetros:");
            System.out.println("  <indexPath>  - Ruta al directorio del índice");
            System.out.println("  <field>      - Campo a buscar (ej: title, abstract, authors, full_text, journal)");
            System.out.println("  <queryText>  - Texto de consulta a buscar");
            System.out.println("  [topN]       - Número de documentos a mostrar (default: 10)");
            return;
        }

        String indexPath = args[0];
        String field = args[1];
        String queryText = args[2];
        int topN = 10;
        if (args.length == 4){
            try{
                topN = Integer.parseInt(args[3]);
                if(topN <= 0){
                    System.err.println("Error: El parámetro topN debe ser mayor que 0");
                    return;
                }
            } catch (NumberFormatException e){
                System.err.println("Error: El parámetro topN debe ser un número entero");
                e.printStackTrace(System.err);
                return;
            }
        }

        IndexReader reader = null;
        Directory dir = null;
        IndexSearcher searcher = null;
        StoredFields storedFields = null;
        QueryParser parser;
        Query query = null;

        try{
            dir = FSDirectory.open(Paths.get(indexPath));
            reader = DirectoryReader.open(dir);

        } catch(CorruptIndexException e1) {
            System.err.println("Error: El índice está corrupto: " + e1);
            e1.printStackTrace(System.err);
        } catch(IOException e1) {
            System.err.println("Error: No se puede abrir el índice: " + e1);
            e1.printStackTrace(System.err);
        }

        try{
            storedFields = reader.storedFields();
        } catch (IOException e){
            System.err.println("Error: No se pueden leer los campos almacenados: " + e);
            e.printStackTrace(System.err);
        }

        searcher = new IndexSearcher(reader);
        parser = new QueryParser(field, new StandardAnalyzer());

        try{
            query = parser.parse(queryText);
        } catch (ParseException e){
            System.err.println("Error: No se puede parsear la consulta: " + e);
            e.printStackTrace(System.err);
        }

        TopDocs topDocs = null;

        try{
            topDocs = searcher.search(query, topN);
        }catch (IOException e1){
            System.err.println("Error: No se puede realizar la búsqueda: " + e1);
            e1.printStackTrace(System.err);
        }

        System.out.println(
                "\n" + topDocs.totalHits + " resultados para la consulta \"" + query.toString() + "\" en el campo: "
                        + field + "\nmostrando los primeros " + topN + " documentos con doc id, score y contenido de campos");

        System.out.println("\nDetalles de la búsqueda:");
        System.out.println("  Texto de consulta: " + queryText);
        System.out.println("  Objeto consulta: " + query);
        System.out.println("  Campo buscado: " + field);
        System.out.println("-".repeat(80));

        for (int i = 0; i < Math.min(topN, topDocs.totalHits.value); i++){
            try{
                int docId = topDocs.scoreDocs[i].doc;
                float score = topDocs.scoreDocs[i].score;
                String cordUid = storedFields.document(docId).get("cord_uid");
                String fieldContent = storedFields.document(docId).get(field);

                System.out.println("\n[" + (i + 1) + "] ID del documento: " + docId + " -- cord_uid: " + cordUid + " -- Score: " + String.format("%.6f", score));

                // Mostrar contenido del campo solicitado
                if(fieldContent != null){
                    // Limitar longitud para mejor legibilidad
                    String displayContent = fieldContent.length() > 100 ? fieldContent.substring(0, 100) + "..." : fieldContent;
                    System.out.println("  " + field + ": " + displayContent);
                }
            } catch (CorruptIndexException e){
                System.err.println("Error: Índice corrupto al leer documento: " + e);
                e.printStackTrace(System.err);
            } catch (IOException e){
                System.err.println("Error: No se puede leer el documento: " + e);
                e.printStackTrace(System.err);
            }
        }

        try{
            reader.close();
            dir.close();
        }catch (IOException e){
            System.err.println("Error: No se pueden cerrar los recursos: " + e);
            e.printStackTrace(System.err);
        }
    }

}

