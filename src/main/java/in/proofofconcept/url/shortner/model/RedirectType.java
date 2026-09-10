package in.proofofconcept.url.shortner.model;

import org.springframework.http.HttpStatus;

public enum RedirectType {
    PERMANENT_301(HttpStatus.MOVED_PERMANENTLY),
    TEMPORARY_302(HttpStatus.FOUND);

    private final HttpStatus httpStatus;

    RedirectType(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
