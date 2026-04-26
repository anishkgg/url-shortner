package in.proofofconcept.url.shortner.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.nio.file.attribute.UserPrincipalNotFoundException;



public class CustomException extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }
}
