package at.technikum.swen3.endpoint;


import at.technikum.swen3.security.JwtUtil;
import at.technikum.swen3.service.IDocumentService;
import at.technikum.swen3.service.IUserService;
import at.technikum.swen3.service.mapper.DocumentMapper;
import at.technikum.swen3.service.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.invoke.MethodHandles;

@RestController
@RequestMapping(value = "/api/v1/document")
public class DocumentEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final IDocumentService documentService;
    private final DocumentMapper documentMapper;
    private final JwtUtil jwtUtil;

    @Autowired
    public DocumentEndpoint(IDocumentService documentService, DocumentMapper documentMapper, JwtUtil jwtUtil) {
        this.documentService = documentService;
        this.documentMapper = documentMapper;
        this.jwtUtil = jwtUtil;
    }

}
