package es.udc.fi.irudc.pri.movies;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// This annotation is used to tell Jackson (a JSON parsing library) to ignore any properties in the JSON 
// that are not mapped to the fields in this class. This prevents issues when the incoming JSON has extra fields.
@JsonIgnoreProperties(ignoreUnknown = true) 
// This is a Java record, which is a concise way to define an immutable data class. 
// In this case, it's representing a MovieScript with a title, date, and a list of scenes.
public record MovieScript(String title, LocalDate date, List<Scene> scenes) {

    // This nested record defines a Scene in the movie script. It contains a transition (String),
    // a header (String), and a list of contents that make up the scene.
    public static record Scene(String transition, String header, List<SceneContent> contents) {}

    // This annotation is added to the SceneContent record as well, meaning that if extra properties exist
    // in the JSON for SceneContent, they will also be ignored to avoid parsing errors.
    @JsonIgnoreProperties(ignoreUnknown = true)
    // This record defines the content of a scene, where each piece of content has a type (String)
    // and the corresponding text (String).
    public static record SceneContent(String type, String text) {}
}

