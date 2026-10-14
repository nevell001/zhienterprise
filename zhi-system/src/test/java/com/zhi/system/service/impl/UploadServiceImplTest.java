package com.zhi.system.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.zhi.system.domain.Upload;
import com.zhi.system.mapper.UploadMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UploadServiceImpl 单元测试
 *
 * @author nevell
 */
@ExtendWith(MockitoExtension.class)
class UploadServiceImplTest
{
    @Mock
    private UploadMapper uploadMapper;

    @InjectMocks
    private UploadServiceImpl uploadService;

    @Test
    void testRecordUpload()
    {
        Upload upload = new Upload();
        upload.setFileName("a.png");
        upload.setUrl("/profile/upload/a.png");
        upload.setFileSize(1024L);
        when(uploadMapper.insertUpload(upload)).thenReturn(1);

        assertEquals(1, uploadService.recordUpload(upload));
        verify(uploadMapper).insertUpload(upload);
    }

    @Test
    void testRecordUploadNullFile()
    {
        assertEquals(0, uploadService.recordUpload(null));
        Upload empty = new Upload();
        assertEquals(0, uploadService.recordUpload(empty));
        verify(uploadMapper, never()).insertUpload(any());
    }

    @Test
    void testDeleteUploadRecordNotFound()
    {
        when(uploadMapper.selectUploadById(1L)).thenReturn(null);
        assertEquals(0, uploadService.deleteUploadById(1L));
        verify(uploadMapper, never()).deleteUploadById(1L);
    }

    @Test
    void testDeleteUploadRemovesRecord()
    {
        Upload upload = new Upload();
        upload.setId(1L);
        upload.setUrl("/profile/upload/x.png");
        when(uploadMapper.selectUploadById(1L)).thenReturn(upload);
        when(uploadMapper.deleteUploadById(1L)).thenReturn(1);

        assertEquals(1, uploadService.deleteUploadById(1L));
        verify(uploadMapper).deleteUploadById(1L);
    }
}
