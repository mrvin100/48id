package io.k48.fortyeightid.provisioning.internal;

record CsvRowError(
        int row,
        String matricule,
        String error) {
}
