package at.technikum.swen3;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;

import at.technikum.swen3.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.ObjectWriteResponse;
import org.mockito.ArgumentCaptor;

class S3ServiceTest {

    private S3Service s3Service;
    private MinioClient minioClient;

    @BeforeEach
    void setUp() throws Exception {
        s3Service = new S3Service();

        minioClient = mock(MinioClient.class);

        Field clientField = S3Service.class.getDeclaredField("minioClient");
        clientField.setAccessible(true);
        clientField.set(s3Service, minioClient);

        Field bucketField = S3Service.class.getDeclaredField("bucketName");
        bucketField.setAccessible(true);
        bucketField.set(s3Service, "test-bucket");
    }

    @Test
    void uploadFile_uploadsAndReturnsKey() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.txt");
        byte[] content = "hello".getBytes();
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        when(file.getSize()).thenReturn((long) content.length);
        when(file.getContentType()).thenReturn("text/plain");

        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        String s3Key = s3Service.uploadFile(file);

        assertNotNull(s3Key);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        assertTrue(captor.getValue().object().endsWith(".txt"));
    }

    @Test
    void uploadFile_throwsRuntimeException_whenMinioFails() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("file.txt");
        byte[] content = "hello".getBytes();
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        when(file.getSize()).thenReturn((long) content.length);
        when(file.getContentType()).thenReturn("text/plain");

        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("minio error"));

        assertThrows(RuntimeException.class, () -> s3Service.uploadFile(file));
    }

    @Test
    void downloadFile_returnsStream() throws Exception {
        GetObjectResponse getResp = mock(GetObjectResponse.class);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(getResp);

        InputStream in = s3Service.downloadFile("some-key");
        assertNotNull(in);

        verify(minioClient).getObject(any(GetObjectArgs.class));
    }

    @Test
    void downloadFile_throwsRuntimeException_whenMinioFails() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("get error"));

        assertThrows(RuntimeException.class, () -> s3Service.downloadFile("some-key"));
    }

    @Test
    void deleteFile_invokesRemoveObject() throws Exception {
        s3Service.deleteFile("delete-key");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void deleteFile_throwsRuntimeException_whenMinioFails() throws Exception {
        doThrow(new RuntimeException("remove error")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThrows(RuntimeException.class, () -> s3Service.deleteFile("delete-key"));
    }

    @Test
    void getObjectMetadata_returnsStatResponse() throws Exception {
        StatObjectResponse resp = mock(StatObjectResponse.class);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(resp);

        StatObjectResponse result = s3Service.getObjectMetadata("meta-key");

        assertSame(resp, result);
        verify(minioClient).statObject(any(StatObjectArgs.class));
    }

    @Test
    void getObjectMetadata_throwsRuntimeException_whenMinioFails() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(new RuntimeException("stat error"));

        assertThrows(RuntimeException.class, () -> s3Service.getObjectMetadata("meta-key"));
    }
}
