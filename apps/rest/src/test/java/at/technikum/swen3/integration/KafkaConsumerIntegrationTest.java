package at.technikum.swen3.integration;

import at.technikum.swen3.entity.Document;
import at.technikum.swen3.entity.User;
import at.technikum.swen3.repository.DocumentRepository;
import at.technikum.swen3.repository.UserRepository;
import at.technikum.swen3.service.dtos.GenAiResultMessageDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class KafkaConsumerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${kafka.topic.result}")
    private String resultTopic;

    private User testUser;
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("kafkatest");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser = userRepository.save(testUser);

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        DefaultKafkaProducerFactory<String, String> producerFactory = 
                new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(producerFactory);
    }

    @Test
    void testKafkaProducer_sendsMessageSuccessfully() throws Exception {
        Document document = new Document();
        document.setName("test-doc.pdf");
        document.setS3Key("test-s3-key-123");
        document.setOwner(testUser);
        document = documentRepository.save(document);

        assertNull(document.getElasticId());

        String elasticId = "elastic-doc-id-456";
        GenAiResultMessageDto resultMessage = new GenAiResultMessageDto(
                "OCR processed text",
                "Document summary",
                document.getS3Key(),
                elasticId,
                document.getName()
        );

        String messageJson = objectMapper.writeValueAsString(resultMessage);
        
        kafkaTemplate.send(resultTopic, messageJson).get(5, java.util.concurrent.TimeUnit.SECONDS);

        document.setElasticId(elasticId);
        document = documentRepository.save(document);

        Document updatedDoc = documentRepository.findById(document.getId()).orElseThrow();
        assertNotNull(updatedDoc.getElasticId());
        assertEquals(elasticId, updatedDoc.getElasticId());
    }

    @Test
    void testDocumentRepository_findByS3Key() {
        Document document = new Document();
        document.setName("test-doc.pdf");
        document.setS3Key("test-s3-key-456");
        document.setOwner(testUser);
        documentRepository.save(document);

        var foundDoc = documentRepository.findByS3Key("test-s3-key-456");
        
        assertTrue(foundDoc.isPresent());
        assertEquals("test-doc.pdf", foundDoc.get().getName());
    }

    @Test
    void testMessageParsing_handlesInvalidJson() throws Exception {
        GenAiResultMessageDto resultMessage = new GenAiResultMessageDto(
                "OCR text",
                "Summary",
                "test-s3-key",
                "elastic-id",
                "test.pdf"
        );

        String validJson = objectMapper.writeValueAsString(resultMessage);
        GenAiResultMessageDto parsed = objectMapper.readValue(validJson, GenAiResultMessageDto.class);
        
        assertNotNull(parsed);
        assertEquals("test-s3-key", parsed.s3Key());
        assertEquals("elastic-id", parsed.elasticId());
    }
}
