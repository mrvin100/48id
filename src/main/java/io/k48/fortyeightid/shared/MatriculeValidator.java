package io.k48.fortyeightid.shared;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validates K48 matricule format: K48-B{n}-{seq}
 * Accessible to all modules via the shared OPEN module.
 */
public class MatriculeValidator {

    private static final Pattern FORMAT = Pattern.compile("^K48-B[0-9]{1,4}-[0-9]+$");

    private MatriculeValidator() {}

    /**
     * Validates that the matricule matches the expected format and is consistent with the given batch.
     *
     * @param matricule the matricule to validate (e.g. "K48-B1-12")
     * @param batch     the batch the user belongs to (e.g. "B1")
     * @return empty if valid, or an error message string if invalid
     */
    public static Optional<String> validate(String matricule, String batch) {
        if (matricule == null || !FORMAT.matcher(matricule).matches()) {
            return Optional.of("Matricule '" + matricule + "' does not match required format K48-B{n}-{seq}");
        }
        if (batch != null) {
            var expectedPrefix = "K48-" + batch + "-";
            if (!matricule.startsWith(expectedPrefix)) {
                var actualPrefix = matricule.substring(0, matricule.lastIndexOf('-'));
                return Optional.of("Matricule prefix '" + actualPrefix + "' does not match batch '" + batch + "'");
            }
        }
        return Optional.empty();
    }
}
