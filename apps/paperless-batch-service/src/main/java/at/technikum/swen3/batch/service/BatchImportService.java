package at.technikum.swen3.batch.service;

import at.technikum.swen3.batch.config.BatchProperties;
import at.technikum.swen3.batch.service.model.ImportPayload;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
public class BatchImportService {

    private static final Logger LOG = LoggerFactory.getLogger(BatchImportService.class);

    private final BatchProperties batchProperties;
    private final XmlImportParser xmlImportParser;
    private final ImportRestClient importRestClient;

    public BatchImportService(BatchProperties batchProperties, XmlImportParser xmlImportParser, ImportRestClient importRestClient) {
        this.batchProperties = batchProperties;
        this.xmlImportParser = xmlImportParser;
        this.importRestClient = importRestClient;
    }

    public void runImportBatch() {
        Path folder = batchProperties.batchFolder();
        LOG.info("Running batch import for folder {}", folder);
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            LOG.error("Configured batch folder does not exist or is not a directory: {}", folder);
            return;
        }

        try {
            Files.list(folder)
                    .filter(this::isXmlFile)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(this::processFileSafely);
        } catch (IOException ex) {
            LOG.error("Failed to read batch folder {}: {}", folder, ex.getMessage());
        }
    }

    private boolean isXmlFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".xml");
    }

    private void processFileSafely(Path file) {
        try {
            ImportPayload payload = xmlImportParser.parse(file);
            importRestClient.send(payload);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            LOG.error("Failed to parse XML file {}: {}", file.getFileName(), ex.getMessage());
        } catch (RuntimeException ex) {
            LOG.error("Failed to process file {}: {}", file.getFileName(), ex.getMessage());
        }
    }
}
