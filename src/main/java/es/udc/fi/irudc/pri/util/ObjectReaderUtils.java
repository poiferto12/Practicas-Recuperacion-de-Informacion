package es.udc.fi.irudc.pri.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Class with utility methods for Jackson {@link ObjectReader} objects
 */
public class ObjectReaderUtils {

  private ObjectReaderUtils() {}

  /**
   * Convenience method to read all values of a file to a {@link List}. Using this
   * methods addresses the shortcomings of Java's type inference when chaining
   * method calls and produces a {@link List} ot the target type T instead of a
   * List of Object
   *
   * @param <T>    The type of the objects to read (and the elements of the
   *               resulting List)
   * @param path   The path of the input file
   * @param reader The object reader
   * @return a list of all the objects of type T parsed from the file
   * @throws IOException exceptions are propagated
   */
  public static <T> List<T> readAllValues(Path path, ObjectReader reader) throws IOException {
    // This method is generic, meaning it can work with any type T.
    // The <T> allows this method to return a list of any type (e.g., Movie, MovieScript, etc.).
    
    // 'path.toFile()' converts the Path object to a File object. The ObjectReader is used to read
    // the contents of the file and map them to instances of type T.
    // 'reader.readValues()' starts reading from the file, expecting multiple values that map to type T.
    // 'readAll()' reads all the values at once and returns them as a List<T>.
    return reader.<T>readValues(path.toFile()).readAll();
}
}
