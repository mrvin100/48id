package io.k48.fortyeightid.provisioning.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CsvImportControllerTest {

    @Mock
    private CsvImportService csvImportService;

    @InjectMocks
    private CsvImportController csvImportController;

    @Test
    void downloadTemplate_returnsCsvFileWithCorrectHeaders() {
        var expectedTemplate = "matricule,email,name,phone,batch,specialization\nK48-B1-1,john.doe@k48.io,John Doe,+237600000000,2024,Software Engineering\n";
        when(csvImportService.generateTemplate()).thenReturn(expectedTemplate);

        ResponseEntity<byte[]> response = csvImportController.downloadTemplate();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
        assertThat(response.getHeaders().getContentDisposition().getFilename()).isEqualTo("48id_import_template.csv");
        assertThat(new String(response.getBody())).isEqualTo(expectedTemplate);

        verify(csvImportService, times(1)).generateTemplate();
    }

    @Test
    void downloadTemplate_hasCorrectContentDisposition() {
        var template = "matricule,email,name,phone,batch,specialization\nexample\n";
        when(csvImportService.generateTemplate()).thenReturn(template);

        ResponseEntity<byte[]> response = csvImportController.downloadTemplate();

        HttpHeaders headers = response.getHeaders();
        assertThat(headers.getContentDisposition().toString()).contains("attachment");
        assertThat(headers.getContentDisposition().getFilename()).isEqualTo("48id_import_template.csv");
    }
}
