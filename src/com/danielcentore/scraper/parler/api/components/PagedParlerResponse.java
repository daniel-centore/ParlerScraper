package com.danielcentore.scraper.parler.api.components;

import com.danielcentore.scraper.parler.api.ParlerTime;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents any type of paged response coming from Parler
 *
 * @author Daniel Centore
 */
public class PagedParlerResponse extends ParlerResponse {

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
    
    public boolean getLast() {
        return last == null ? false : last;
    }
}
