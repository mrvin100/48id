package io.k48.fortyeightid.provisioning.internal;

record CsvRow(
        int rowNumber,
        String matricule,
        String email,
        String name,
        String phone,
        String batch,
        String specialization) {
}
