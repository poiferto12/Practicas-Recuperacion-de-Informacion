package es.udc.fi.irudc.pri.Cord19;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import es.udc.fi.irudc.pri.util.ObjectReaderUtils;

public class ReadCord19 {

    /*
     * This is the default path for the CORD-19 collection.
     * Note: It only works when running in an IDE or using Maven with 'exec:java'.
     * When packaged into a jar, this path won't be accessible as expected.
     */
    private static final Path DEFAULT_COLLECTION_PATH =Paths.get(System.getProperty("user.home"), "Documents", "GIF", "3eiro", "RI", "resourcesCord19", "2020-07-16");

    // The name of the file containing metadata about the articles (a CSV file).
    private static String METADATA_FILE_NAME = "metadata.csv";

    // An ObjectReader used to read JSON and map it to the BodyTextCord19 class.
    // JsonMapper finds all required modules for parsing the JSON.
    private static final ObjectReader BODY_TEXT_READER = JsonMapper.builder().findAndAddModules().build()
            .readerFor(BodyTextCord19.class);

    /**
     * Utility method to read a full-text JSON file and return its content as a String.
     *
     * @param jsonPath the path to the JSON file
     * @return the content of the full text as a String
     */
    public static String readFullText(Path jsonPath) {
        if (jsonPath == null || !Files.exists(jsonPath)) {
            return "";
        }

        BodyTextCord19 bodyText;
        try {
            // Reads the JSON file and converts it into a BodyTextCord19 object.
            bodyText = BODY_TEXT_READER.readValue(jsonPath.toFile());
        } catch (IOException e) {
            // If reading the file fails, an error message is printed and an empty string is returned.
            System.err.println("Error reading full-text JSON file: " + jsonPath);
            e.printStackTrace();
            return "";
        }

        // Build a string representation of the full text by appending all text sections from body_text.
        StringBuilder sb = new StringBuilder();
        if (bodyText.bodyText() != null) {
            for (BodyTextCord19.Section section : bodyText.bodyText()) {
                if (section.text() != null && !section.text().isEmpty()) {
                    sb.append(section.text());
                    sb.append('\n');
                }
            }
        }
        return sb.toString();
    }

    /**
     * Metodo para recoller path completo dos json
     *
     * @param article o articulo do que queremos o path
     * @param collectionPath o path base da colecion
     * @return o path, ou null se non existe
     */
    public static Path getFullTextJsonPath(Cord19 article, Path collectionPath) {
        // Priemiro o PMC se existe body text
        if (article.pmcJsonText() != null && !article.pmcJsonText().isEmpty()) {
            // Coller solo o primeiro se existen varios separados por ;
            String pmcFile = article.pmcJsonText().split(";")[0].trim();
            if (!pmcFile.isEmpty()) {
                Path pmcPath = collectionPath.resolve(pmcFile);
                if (Files.exists(pmcPath)) {
                    return pmcPath;
                }
            }
        }

        // Se non hay PMC, buscase o do PDF
        if (article.pdfJsonText() != null && !article.pdfJsonText().isEmpty()) {
            String pdfFile = article.pdfJsonText().split(";")[0].trim();
            if (!pdfFile.isEmpty()) {
                Path pdfPath = collectionPath.resolve(pdfFile);
                if (Files.exists(pdfPath)) {
                    return pdfPath;
                }
            }
        }

        return null;
    }

    public static void main(String[] args) {
        Path collectionPath;

        // Determine if a collection path is provided as an argument; otherwise, use the default.
        if (args.length > 0) {
            collectionPath = Paths.get(args[0]);
        } else {
            collectionPath = DEFAULT_COLLECTION_PATH;
        }

        // Combine the collection path with the metadata file name to get the full path to the CSV.
        Path metadataPath = collectionPath.resolve(METADATA_FILE_NAME);

        /*
         * Define the schema of the CSV file:
         *   - emptySchema(): starts with an empty schema template.
         *   - withHeader(): uses the first row of the CSV as the header containing column names.
         *   - withArrayElementSeparator("; "): defines that multi-valued fields in the CSV are separated by "; ".
         */
        CsvSchema schema = CsvSchema.emptySchema().withHeader().withArrayElementSeparator("; ");
        /*
         * Create an ObjectReader, which will parse the CSV file and map each row
         * to an instance of the Cord19 class using the previously defined schema.
         */
        ObjectReader reader = new CsvMapper().readerFor(Cord19.class).with(schema);

        List<Cord19> articles;

        try {
            // Reads and parses all values from the CSV file using the ObjectReader.
            articles = ObjectReaderUtils.readAllValues(metadataPath, reader);
        } catch (IOException ex) {
            // In case of an error during file reading/parsing, print an error message.
            System.err.println("Error when trying to read and parse the input file");
            ex.printStackTrace();
            return;
        }

        System.out.println("Total de artículos leídos: " + articles.size());

        // Map para indicar os articulos unicos
        Map<String, Cord19> uniqueArticles = new HashMap<>();

        for (Cord19 article : articles) {
            // Solo se añade o primeiro que teña ese uid

            // // // // // Teño q cambiar o nombre de cordUi a cordUid despos de acabar esto // // // // //
            if (article.cordUi() != null && !uniqueArticles.containsKey(article.cordUi())) {
                uniqueArticles.put(article.cordUi(), article);
            }
        }

        System.out.println("Artículos únicos: " + uniqueArticles.size());

        // En vez de mostrar os 200k, solo mostro un par
        int count = 0;
        int maxToShow = 3;  // Mostrar solo os 3 primeiros

        // Para cada arrticulo unico, mostrar info e o bodytext se ta disponible
        for (Cord19 article : uniqueArticles.values()) {
            if (count >= maxToShow) {
                break;
            }
            System.out.println("\n=== Artículo " + (count + 1) + " ===");
            System.out.println("CORD_UID: " + article.cordUi());
            System.out.println("Título: " + article.title());
            System.out.println("Autores: " + article.authors());
            System.out.println("Journal: " + article.journal());
            System.out.println("Fecha publicación: " + article.publishTime());
            System.out.println("DOI: " + article.doi());
            System.out.println("URL: " + article.url());
            System.out.println("Licencia: " + article.license());

            // Mostrar solo una parte do abstract
            if (article.abstractText() != null && !article.abstractText().isEmpty()) {
                String abstractPreview = article.abstractText().substring(
                        0, Math.min(200, article.abstractText().length()));
                System.out.println("Abstract (primeros 200 chars): " + abstractPreview + "...");
            }

            // Printear solo un trozo do body text ou full text, sigo sin saber que nombre ten a vd
            Path jsonPath = getFullTextJsonPath(article, collectionPath);
            if (jsonPath != null) {
                String fullText = readFullText(jsonPath);
                if (!fullText.isEmpty()) {
                    String fullTextPreview = fullText.substring(
                            0, Math.min(300, fullText.length()));
                    System.out.println("Body Text (primeros 300 chars): " + fullTextPreview + "...");
                    System.out.println("Longitud total del texto: " + fullText.length() + " caracteres");
                }
            } else {
                System.out.println("No se encontró archivo JSON con texto");
            }

            count++;
        }

        System.out.println("\n=== completado ===");
    }
}
