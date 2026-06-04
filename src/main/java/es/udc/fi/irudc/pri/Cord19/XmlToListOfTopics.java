package es.udc.fi.irudc.pri.Cord19;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
record Topics(
        @JacksonXmlElementWrapper(useWrapping = false) 
        @JacksonXmlProperty(localName = "topic") 
        List<Topic> topics) {
        record Topic(String number, String query, String question, String narrative) {
    }
}

public class XmlToListOfTopics {

    // Path to the folder where the xml of topics is saved
    private static final Path DEFAULT_COLLECTION_PATH = Paths.get("src", "main",
            "resources");

    public static void main(String[] args)
            throws StreamReadException, DatabindException, IOException {

        XmlMapper mapper = XmlMapper.builder().build();
        Topics topics = mapper.readValue((DEFAULT_COLLECTION_PATH.resolve("topics-rnd5.xml"))
                .toFile(), Topics.class);
        for (Topics.Topic topic : topics.topics()) {
            System.out.println("Topic number: " + topic.number());
            System.out.println("Topic query: " + topic.query());
            System.out.println("Topic question: " + topic.question());
            System.out.println("Topic narrative: " + topic.narrative());
            System.out.println();
        }
    }
}