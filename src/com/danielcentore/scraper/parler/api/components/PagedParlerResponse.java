package com.danielcentore.scraper.parler.api.components;

import com.danielcentore.scraper.parler.api.ParlerTime;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PagedParlerResponse {

    @JsonProperty("last")
    Boolean last;

    @JsonProperty("prev")
    String prev;

    @JsonProperty("next")
    String next;

    public ParlerTime getCurrentKey() {
        return ParlerTime.fromParlerTimestamp(prev);
    }

    public ParlerTime getNextKey() {
        return ParlerTime.fromParlerTimestamp(next);
    }
}
