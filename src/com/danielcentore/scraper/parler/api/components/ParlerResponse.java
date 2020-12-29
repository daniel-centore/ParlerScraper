package com.danielcentore.scraper.parler.api.components;

import javax.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ParlerResponse {
    // This is used for errors
    @JsonProperty("message")
    String message;
    
    @Transient
    public String getMessage() {
        return message;
    }
}
