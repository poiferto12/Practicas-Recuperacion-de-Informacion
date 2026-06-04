package es.udc.fi.irudc.pri.Cord19;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import es.udc.fi.irudc.pri.util.ObjectReaderUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

public class IndexCord19 {

    // Configuración por defecto
    private static final Path DEFAULT_COLLECTION_PATH = Paths.get(System.getProperty("user.home"),
            "Documents", "GIF", "3eiro", "RI", "resourcesCord19", "2020-07-16");
    private static final Path DEFAULT_INDEX_PATH = Paths.get("index-cord19");
    private static final String METADATA_FILE_NAME = "metadata.csv";
    private static final int DEFAULT_NUM_THREADS = 4;
    private static final int COMMIT_INTERVAL = 1000; // Commit cada 1000 documentos

    // Contador atómico para progreso
    private static final java.util.concurrent.atomic.AtomicInteger processedCount =
            new java.util.concurrent.atomic.AtomicInteger(0);
    private static volatile boolean shutdownRequested = false;


    //Clase worker

    private static class IndexWorker implements Runnable {
        private final List<Cord19> articles;
        private final Path collectionPath;
        private final IndexWriter writer;
        private final boolean storeFullText;
        private final int totalArticles;

        public IndexWorker(List<Cord19> articles, Path collectionPath, IndexWriter writer, boolean storeFullText, int totalArticles) {
            this.articles = articles;
            this.collectionPath = collectionPath;
            this.writer = writer;
            this.storeFullText = storeFullText;
            this.totalArticles = totalArticles;
        }

        @Override
        public void run() {
            for (Cord19 article : articles){
                if (shutdownRequested){
                    System.out.println("\nInterrupción en hilo " + Thread.currentThread().getName());
                    break;
                }

                try{
                    indexArticle(article, collectionPath, writer, storeFullText);

                    int count = processedCount.incrementAndGet();

                    // Mostrar progreso cada 100 documentos
                    if (count % 100 == 0){
                        double percentage = (count * 100.0) / totalArticles;
                        System.out.printf("\rProgreso: %d/%d documentos (%.2f%%)  ",
                                count, totalArticles, percentage);
                    }

                    // Commit periódico cada COMMIT_INTERVAL documentos
                    if (count % COMMIT_INTERVAL == 0){
                        synchronized (writer) {
                            try{
                                writer.commit();
                                System.out.printf("\n[Commit automático realizado en documento %d]\n", count);
                            } catch (IOException e) {
                                System.err.println("\nError en commit periódico: " + e.getMessage());
                            }
                        }
                    }

                } catch (IOException e){
                    System.err.println("\nError indexando artículo " + article.cordUi() + ": " + e.getMessage());
                }
            }
        }
    }

    private static void indexArticle(Cord19 article, Path collectionPath, IndexWriter writer, boolean storeFullText) throws IOException {
        Document doc = new Document();

        // Campo cord_uid: StringField
        if (article.cordUi() != null){
            doc.add(new StringField("cord_uid", article.cordUi(), Field.Store.YES));
        }

        // Campo title: TextField
        if (article.title() != null) {
            FieldType titleType = new FieldType(TextField.TYPE_STORED);
            titleType.setStoreTermVectors(true);
            titleType.setStoreTermVectorPositions(true);
            titleType.freeze();
            doc.add(new Field("title", article.title(), titleType));
        }

        // Campo authors: TextField
        if (article.authors() != null){
            FieldType authorsType = new FieldType(TextField.TYPE_STORED);
            authorsType.setStoreTermVectors(true);
            authorsType.setStoreTermVectorPositions(true);
            authorsType.freeze();
            doc.add(new Field("authors", article.authors(), authorsType));
        }

        // Campo abstract: TextField
        if (article.abstractText() != null){
            FieldType abstractType = new FieldType(TextField.TYPE_STORED);
            abstractType.setStoreTermVectors(true);
            abstractType.setStoreTermVectorPositions(true);
            abstractType.freeze();
            doc.add(new Field("abstract", article.abstractText(), abstractType));
        }

        // Campo publish_time: StringFiel
        if (article.publishTime() != null){
            doc.add(new StringField("publish_time", article.publishTime(), Field.Store.YES));
        }

        // Campo journal: TextField
        if (article.journal() != null) {
            doc.add(new TextField("journal", article.journal(), Field.Store.YES));
        }

        // Campo doi: StringField
        if (article.doi() != null){
            doc.add(new StringField("doi", article.doi(), Field.Store.YES));
        }

        // Campo url: StringField
        if (article.url() != null){
            doc.add(new StringField("url", article.url(), Field.Store.YES));
        }

        // Campo license: StringField
        if (article.license() != null){
            doc.add(new StringField("license", article.license(), Field.Store.YES));
        }

        // Campo full-text: TextField
        // Se usan métodos de ReadCord19
        Path jsonPath = ReadCord19.getFullTextJsonPath(article, collectionPath);
        if (jsonPath != null){
            String fullText = ReadCord19.readFullText(jsonPath);
            if (!fullText.isEmpty()){
                Field.Store store = storeFullText ? Field.Store.YES : Field.Store.NO;
                doc.add(new TextField("full_text", fullText, store));
            }
        }

        // Añadir el documento al índice
        synchronized (writer){
            writer.addDocument(doc);
        }
    }

    // Filtro para no añadir los duplicaods
    private static List<Cord19> filterUniqueArticles(List<Cord19> articles) {
        Map<String, Cord19> uniqueMap = new LinkedHashMap<>();
        for (Cord19 article : articles){
            if (article.cordUi() != null && !uniqueMap.containsKey(article.cordUi())){
                uniqueMap.put(article.cordUi(), article);
            }
        }
        return new ArrayList<>(uniqueMap.values());
    }

    // Dividir en sublistas para los hilos
    private static <T> List<List<T>> partitionList(List<T> list, int numPartitions) {
        List<List<T>> partitions = new ArrayList<>();
        int size = list.size();
        int partitionSize = (size + numPartitions - 1) / numPartitions;

        for (int i = 0; i < size; i += partitionSize){
            partitions.add(list.subList(i, Math.min(i + partitionSize, size)));
        }

        return partitions;
    }

    public static void indexCollection(Path collectionPath, Path indexPath, int numThreads, boolean storeFullText) throws IOException {

        System.out.println("=== Iniciando indexación CORD-19 ===");
        System.out.println("Colección: " + collectionPath);
        System.out.println("Índice: " + indexPath);
        System.out.println("Hilos: " + numThreads);
        System.out.println("Almacenar full-text: " + storeFullText);

        long startTime = System.currentTimeMillis();

        // Leer metadata.csv
        Path metadataPath = collectionPath.resolve(METADATA_FILE_NAME);
        CsvSchema schema = CsvSchema.emptySchema().withHeader().withArrayElementSeparator("; ");
        ObjectReader reader = new CsvMapper().readerFor(Cord19.class).with(schema);

        List<Cord19> articles;
        try{
            articles = ObjectReaderUtils.readAllValues(metadataPath, reader);
            System.out.println("Artículos leídos del CSV: " + articles.size());
        }catch (IOException ex) {
            System.err.println("Error leyendo metadata.csv");
            throw ex;
        }

        // Filtrar artículos
        List<Cord19> uniqueArticles = filterUniqueArticles(articles);
        System.out.println("Artículos únicos (por cord_uid): " + uniqueArticles.size());

        // Configurar Lucene IndexWriter
        Directory dir = FSDirectory.open(indexPath);
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(OpenMode.CREATE);

        IndexWriter writer = new IndexWriter(dir, config);

        // Indexación con concurrencia
        ExecutorService executor = null;
        try{
            if (numThreads == 1) {
                for (Cord19 article : uniqueArticles){
                    if (shutdownRequested){
                        System.out.println("\nInterrupción detectada");
                        break;
                    }
                    indexArticle(article, collectionPath, writer, storeFullText);

                    int count = processedCount.incrementAndGet();
                    if (count % 100 == 0){
                        double percentage = (count * 100.0) / uniqueArticles.size();
                        System.out.printf("\rProgreso: %d/%d documentos (%.2f%%)  ",
                                count, uniqueArticles.size(), percentage);
                    }

                    if (count % COMMIT_INTERVAL == 0){
                        writer.commit();
                        System.out.printf("\n[Commit automático realizado en documento %d]\n", count);
                    }
                }
            } else {
                // Indexación en x hilos
                executor = Executors.newFixedThreadPool(numThreads);
                List<List<Cord19>> partitions = partitionList(uniqueArticles, numThreads);

                List<Future<?>> futures = new ArrayList<>();
                for (List<Cord19> partition : partitions){
                    Future<?> future = executor.submit(new IndexWorker(partition, collectionPath, writer, storeFullText, uniqueArticles.size()));
                    futures.add(future);
                }

                // Esperar a que terminen todos
                for (Future<?> future : futures) {
                    try{
                        future.get();
                    } catch (InterruptedException e){
                        System.err.println("\nHilo interrumpido: " + e.getMessage());
                        shutdownRequested = true;
                        break;
                    } catch (ExecutionException e){
                        System.err.println("\nError en hilo de indexación: " + e.getMessage());
                    }
                }

                executor.shutdown();
                try{
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)){
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e){
                    executor.shutdownNow();
                }
            }
        } finally{
            try{
                writer.commit();
                System.out.println("Commit final realizado correctamente.");
            } catch (IOException e){
                System.err.println("Error en commit final: " + e.getMessage());
            }

            writer.close();
            dir.close();

            if (executor != null && !executor.isShutdown()){
                executor.shutdownNow();
            }
        }

        long endTime = System.currentTimeMillis();
        double timeInSeconds = (endTime - startTime) / 1000.0;

        System.out.println("=========== Indexación completada ===========");
        System.out.println("Documentos indexados: " + processedCount.get() + " de " + uniqueArticles.size());
        System.out.println("Tiempo: " + timeInSeconds + " segundos");

        if (processedCount.get() > 0) {
            double docsPerSecond = processedCount.get() / timeInSeconds;
            System.out.printf("Velocidad: %.2f documentos/segundo\n", docsPerSecond);
        }

        // Mostrar tamaño de indice
        long indexSize = Files.walk(indexPath)
                .filter(Files::isRegularFile)
                .mapToLong(p -> {
                    try{
                        return Files.size(p);
                    } catch (IOException e){
                        return 0;
                    }
                })
                .sum();
        System.out.println("Tamaño del indice: " + (indexSize / (1024.0 * 1024.0)) + " MB");
    }

    public static void main(String[] args) {
        Path collectionPath = DEFAULT_COLLECTION_PATH;
        Path indexPath = DEFAULT_INDEX_PATH;
        int numThreads = DEFAULT_NUM_THREADS;
        boolean storeFullText = false; // Por defecto no almacenar full-text para ahorrar espacio

        // Añadir shutdown hook para manejar Ctrl+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!shutdownRequested){
                System.out.println("\n\nInterrupción detectada");
                shutdownRequested = true;
                try{
                    // Dar tiempo para que los hilos terminen limpiamente
                    Thread.sleep(2000);
                } catch (InterruptedException e){
                    // Ignorar
                }
            }
        }));

        // Parsing de argumentos de línea de comandos
        for (int i = 0; i < args.length; i++){
            switch (args[i]) {
                case "-collection":
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")){
                        collectionPath = Paths.get(args[++i]);
                    }else{
                        System.err.println("Error: -collection requiere una ruta como argumento");
                        printUsage();
                        return;
                    }
                    break;
                case "-index":
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")){
                        indexPath = Paths.get(args[++i]);
                    }else{
                        System.err.println("Error: -index requiere una ruta como argumento");
                        printUsage();
                        return;
                    }
                    break;
                case "-threads":
                    if (i + 1 < args.length && !args[i + 1].startsWith("-")){
                        try{
                            numThreads = Integer.parseInt(args[++i]);
                        }catch (NumberFormatException e){
                            System.err.println("Error: El valor de -threads debe ser un número entero");
                            printUsage();
                            return;
                        }
                    }else{
                        System.err.println("Error: -threads requiere un número como argumento");
                        printUsage();
                        return;
                    }
                    break;
                case "-storeFullText":
                    storeFullText = true;
                    break;
                default:
                    System.err.println("Error: Argumento desconocido: " + args[i]);
                    printUsage();
                    return;
            }
        }

        try{
            indexCollection(collectionPath, indexPath, numThreads, storeFullText);
        } catch (IOException e){
            System.err.println("Error durante la indexación: " + e.getMessage());
            e.printStackTrace();
        }

        if (shutdownRequested){
            System.out.println("\nIndexación interrumpida");
            System.out.println("Se puede continuar la indexación ejecutando el programa de nuevo.");
        }
    }

    private static void printUsage() {
        System.out.println("Uso: java IndexCord19 [opciones]");
        System.out.println("Ejemplo: mvn exec:java -Dexec.mainClass=es.udc.fi.irudc.pri.Cord19.IndexCord19 \"-Dexec.args=-collection C:\\datos\\cord19 -index C:\\indice -threads 8\"");
        System.out.println();
        System.out.println("Opciones:");
        System.out.println("  -collection <ruta>   Ruta a la colección CORD-19 (default: " + DEFAULT_COLLECTION_PATH + ")");
        System.out.println("  -index <ruta>        Ruta donde crear el índice (default: " + DEFAULT_INDEX_PATH + ")");
        System.out.println("  -threads <n>         Número de hilos para indexación (default: " + DEFAULT_NUM_THREADS + ")");
        System.out.println("  -storeFullText       Almacenar el campo full-text (aumenta tamaño del índice)");
    }
}