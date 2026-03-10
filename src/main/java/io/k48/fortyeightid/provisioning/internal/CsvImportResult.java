package io.k48.fortyeightid.provisioning.internal;

import java.util.List;

record CsvImportResult(
        int imported,
        int failed,
        List<CsvRowError> errors) {
}
