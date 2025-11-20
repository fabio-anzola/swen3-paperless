package at.technikum.swen3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.lang.reflect.Field;

import at.technikum.swen3.worker.service.FileDownloadException;
import at.technikum.swen3.worker.service.S3Service;
import org.junit.jupiter.api.Test;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;

class S3ServiceTest {

    private S3Service injectMockClient(MinioClient mockClient) throws Exception {
        S3Service s3Service = new S3Service("http://localhost:9000", "access", "secret", "bucket");
        Field field = S3Service.class.getDeclaredField("minioClient");
        field.setAccessible(true);
        field.set(s3Service, mockClient);
        return s3Service;
    }

    @Test
    void downloadFile_returnsInputStream_whenMinioSucceeds() throws Exception {
        MinioClient mockClient = mock(MinioClient.class);
        byte[] data = "hello".getBytes();

        GetObjectResponse mockResp = mock(GetObjectResponse.class);
        when(mockResp.readAllBytes()).thenReturn(data);

        doReturn(mockResp).when(mockClient).getObject(any(GetObjectArgs.class));

        S3Service s3Service = injectMockClient(mockClient);

        InputStream result = s3Service.downloadFile("some-key");
        byte[] buf = result.readAllBytes();
        assertEquals("hello", new String(buf));
    }

    @Test
    void downloadFile_throwsFileDownloadException_whenMinioFails() throws Exception {
        MinioClient mockClient = mock(MinioClient.class);
        when(mockClient.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("minio error"));

        S3Service s3Service = injectMockClient(mockClient);

        assertThrows(FileDownloadException.class, () -> s3Service.downloadFile("some-key"));
    }

    @Test
    void getObjectMetadata_returnsResponse_whenMinioSucceeds() throws Exception {
        MinioClient mockClient = mock(MinioClient.class);
        StatObjectResponse mockResp = mock(StatObjectResponse.class);
        when(mockClient.statObject(any(StatObjectArgs.class))).thenReturn(mockResp);

        S3Service s3Service = injectMockClient(mockClient);

        StatObjectResponse resp = s3Service.getObjectMetadata("meta-key");
        assertSame(mockResp, resp);
    }

    @Test
    void getObjectMetadata_throwsFileDownloadException_whenMinioFails() throws Exception {
        MinioClient mockClient = mock(MinioClient.class);
        when(mockClient.statObject(any(StatObjectArgs.class))).thenThrow(new RuntimeException("stat error"));

        S3Service s3Service = injectMockClient(mockClient);

        assertThrows(FileDownloadException.class, () -> s3Service.getObjectMetadata("meta-key"));
    }
}
