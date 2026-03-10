package io.k48.fortyeightid.provisioning.internal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalCsvExceptionHandler {

    @ExceptionHandler(CsvImportService.CsvImportException.class)
    ProblemDetail handleCsvImportException(CsvImportService.CsvImportException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problem.setTitle(ex.getErrorCode());
        return problem;
    }
}
