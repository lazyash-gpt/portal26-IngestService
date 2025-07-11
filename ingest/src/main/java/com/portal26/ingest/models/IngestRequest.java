package com.portal26.ingest.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
public class IngestRequest {

    @JsonProperty("event_timestamp")
    private OffsetDateTime eventTimestamp;

    private String body;

}