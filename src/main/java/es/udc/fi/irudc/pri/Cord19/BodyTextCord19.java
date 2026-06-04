package es.udc.fi.irudc.pri.Cord19;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// This annotation is used to tell Jackson (a JSON parsing library) to ignore any properties in the JSON
// that are not mapped to the fields in this class. This prevents issues when the incoming JSON has extra fields.
@JsonIgnoreProperties(ignoreUnknown = true)

// This is a Java record, which is a concise way to define an immutable data class.
public record BodyTextCord19 (@JsonProperty("body_text") List<Section> bodyText){

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record Section(String text) {}
}
