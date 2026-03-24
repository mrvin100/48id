package io.k48.fortyeightid.auth.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
record MatriculeExistsResponse(
        @JsonProperty("exists") boolean exists,
        @JsonProperty("status") String status
) {
    public static MatriculeExistsResponse exists(String status) {
        return new MatriculeExistsResponse(true, status);
    }

    public static MatriculeExistsResponse notExists() {
        return new MatriculeExistsResponse(false, null);
    }
}
