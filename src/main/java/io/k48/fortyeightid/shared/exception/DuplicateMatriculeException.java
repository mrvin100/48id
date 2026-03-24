package io.k48.fortyeightid.shared.exception;

public class DuplicateMatriculeException extends RuntimeException {

    public DuplicateMatriculeException(String matricule) {
        super("Matricule already exists: " + matricule);
    }
}
