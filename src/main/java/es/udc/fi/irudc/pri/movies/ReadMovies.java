package es.udc.fi.irudc.pri.movies;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import es.udc.fi.irudc.pri.util.ObjectReaderUtils;

public class ReadMovies {

    /*
     * This is the default path for the collection of movies.
     * Note: It only works when running in an IDE or using Maven with 'exec:java'.
     * When packaged into a jar, this path won't be accessible as expected.
     */
    private static final Path DEFAULT_COLLECTION_PATH = Paths.get("src", "main", "resources");

    // The name of the file containing metadata about the movies (a CSV file).
    private static String MOVIE_METADATA_FILE_NAME = "movies.csv";

    // An ObjectReader used to read JSON and map it to the MovieScript class.
    // JsonMapper finds all required modules for parsing the JSON.
    private static final ObjectReader SCRIPT_READER = JsonMapper.builder().findAndAddModules().build()
            .readerFor(MovieScript.class);

    /**
     * Utility method to read a script file and return its content as a String.
     *
     * @param scriptPath the path to the script file
     * @return the content of the script as a String
     */
    private static final String readScript(Path scriptPath) {
        MovieScript script;
        try {
            // Reads the JSON script file and converts it into a MovieScript object.
            script = SCRIPT_READER.readValue(scriptPath.toFile());
        } catch (IOException e) {
            // If reading the file fails, an error message is printed and an empty string is returned.
            System.err.println("Error reading script file: " + scriptPath);
            e.printStackTrace();
            return "";
        }

        // Build a string representation of the script by appending title, scene transitions, headers, and contents.
        StringBuilder sb = new StringBuilder();
        sb.append(script.title());
        sb.append('\n');
        for (MovieScript.Scene scene : script.scenes()) {
            sb.append(scene.transition());
            sb.append('\n');
            sb.append(scene.header());
            sb.append('\n');
            for (MovieScript.SceneContent content : scene.contents()) {
                sb.append(content.text());
                sb.append('\n');
            }
        }

        return sb.toString();
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
        Path moviesPath = collectionPath.resolve(MOVIE_METADATA_FILE_NAME);

        /*
         * Define the schema of the CSV file:
         *   - emptySchema(): starts with an empty schema template.
         *   - withHeader(): uses the first row of the CSV as the header containing column names.
         *   - withArrayElementSeparator("; "): defines that multi-valued fields in the CSV are separated by "; ".
         */
        CsvSchema schema = CsvSchema.emptySchema().withHeader().withArrayElementSeparator("; ");
        /*
         * Create an ObjectReader, which will parse the CSV file and map each row
         * to an instance of the Movie class using the previously defined schema.
         */
        ObjectReader reader = new CsvMapper().readerFor(Movie.class).with(schema);

        List<Movie> movies;

        try {
            // Reads and parses all values from the CSV file using the ObjectReader.
            movies = ObjectReaderUtils.readAllValues(moviesPath, reader);
        } catch (IOException ex) {
            // In case of an error during file reading/parsing, print an error message.
            System.err.println("Error when trying to read and parse the input file");
            ex.printStackTrace();
            return;
        }

        // For each movie in the list, print the movie and its script (if available).
        for (Movie movie : movies) {
            System.out.println(movie);
            System.out.println("\n UN ELEMENTO PRINTEADO \n");

            // If the movie has a script file name, read and print the script contents.
            String scriptFilename = movie.script();
            if (scriptFilename != null && !scriptFilename.equals("")) {
                String movieScript = readScript(collectionPath.resolve(scriptFilename));
                System.out.println(movieScript);
                System.out.println(); // Add an empty line after printing each script.
            }
        }
    }
}
