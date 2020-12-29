package com.danielcentore.scraper.parler.api.components;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ParlerMaybeErrorResponse extends ParlerResponse {

}
