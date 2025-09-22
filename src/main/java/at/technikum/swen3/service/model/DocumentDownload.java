package at.technikum.swen3.service.model;

import org.springframework.core.io.Resource;

public record DocumentDownload(Resource body, String contentType, String filename, Long contentLength) {
}