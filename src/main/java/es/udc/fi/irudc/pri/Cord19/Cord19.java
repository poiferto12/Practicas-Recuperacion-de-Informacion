package es.udc.fi.irudc.pri.Cord19;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Cord19 (
        @JsonProperty("cord_uid") String cordUi,
        String title,
        String authors,
        @JsonProperty("abstract") String abstractText,
        @JsonProperty("publish_time") String publishTime,
        String journal,
        String doi,
        @JsonProperty("pdf_json_files") String pdfJsonText,
        @JsonProperty("pmc_json_files") String pmcJsonText,
        String url,
        String license
){}