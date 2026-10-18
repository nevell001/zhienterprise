package com.zhi.system.service.impl;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.zhi.common.exception.ServiceException;
import com.zhi.system.domain.MediaReference;
import com.zhi.system.domain.Upload;
import com.zhi.system.mapper.ArticleMapper;
import com.zhi.system.mapper.FriendLinkMapper;
import com.zhi.system.mapper.PageMapper;
import com.zhi.system.mapper.SettingMapper;
import com.zhi.system.mapper.SysUserMapper;
import com.zhi.system.mapper.UploadMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private PageMapper pageMapper;

    @Mock
    private SettingMapper settingMapper;

    @Mock
    private FriendLinkMapper friendLinkMapper;

    @Mock
    private SysUserMapper sysUserMapper;

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
    @DisplayName("引用检查用文件名汇总所有引用来源")
    void testSelectReferencesAggregatesSources()
    {
        Upload upload = upload(1L, "logo_123.png");
        when(uploadMapper.selectUploadById(1L)).thenReturn(upload);
        when(articleMapper.selectMediaReferences("logo_123.png"))
            .thenReturn(List.of(reference("article", 10L, "关于我们")));
        when(settingMapper.selectMediaReferences("logo_123.png"))
            .thenReturn(List.of(reference("setting", 20L, "site_logo")));

        List<MediaReference> references = uploadService.selectReferences(1L);

        assertEquals(2, references.size());
        assertEquals("article", references.get(0).getReferenceType());
        verify(pageMapper).selectMediaReferences("logo_123.png");
        verify(friendLinkMapper).selectMediaReferences("logo_123.png");
        verify(sysUserMapper).selectMediaReferences("logo_123.png");
    }

    @Test
    @DisplayName("文件名缺失时回退用 URL 的最后一段做引用检查")
    void testSelectReferencesFallsBackToUrl()
    {
        Upload upload = upload(1L, null);
        upload.setUrl("/profile/upload/2026/10/18/logo_123.png");
        when(uploadMapper.selectUploadById(1L)).thenReturn(upload);

        uploadService.selectReferences(1L);

        verify(articleMapper).selectMediaReferences("logo_123.png");
    }

    @Test
    @DisplayName("记录不存在时引用列表为空")
    void testSelectReferencesUploadNotFound()
    {
        when(uploadMapper.selectUploadById(1L)).thenReturn(null);

        assertTrue(uploadService.selectReferences(1L).isEmpty());
        verify(articleMapper, never()).selectMediaReferences(anyString());
    }

    @Test
    @DisplayName("被引用的文件删除时报错并列出引用位置")
    void testDeleteUploadsBlockedByReference()
    {
        when(uploadMapper.selectUploadById(1L)).thenReturn(upload(1L, "logo_123.png"));
        when(articleMapper.selectMediaReferences("logo_123.png"))
            .thenReturn(List.of(reference("article", 10L, "关于我们")));

        ServiceException error = assertThrows(ServiceException.class,
            () -> uploadService.deleteUploads(new Long[] { 1L }));

        assertTrue(error.getMessage().contains("无法删除"));
        assertTrue(error.getMessage().contains("内容「关于我们」"));
        verify(uploadMapper, never()).deleteUploadById(any());
    }

    @Test
    @DisplayName("批量删除先全量校验：任何一个文件被引用都不删除")
    void testDeleteUploadsValidatesWholeBatch()
    {
        when(uploadMapper.selectUploadById(1L)).thenReturn(upload(1L, "free_1.png"));
        when(uploadMapper.selectUploadById(2L)).thenReturn(upload(2L, "used_2.png"));
        when(settingMapper.selectMediaReferences(anyString())).thenAnswer(invocation ->
            "used_2.png".equals(invocation.getArgument(0))
                ? List.of(reference("setting", 20L, "site_logo")) : List.of());

        assertThrows(ServiceException.class,
            () -> uploadService.deleteUploads(new Long[] { 1L, 2L }));

        verify(uploadMapper, never()).deleteUploadById(any());
    }

    @Test
    @DisplayName("无引用时删除记录与物理文件")
    void testDeleteUploadsRemovesRecord()
    {
        when(uploadMapper.selectUploadById(1L)).thenReturn(upload(1L, "free_1.png"));
        when(uploadMapper.deleteUploadById(1L)).thenReturn(1);

        assertEquals(1, uploadService.deleteUploads(new Long[] { 1L }));
        verify(uploadMapper).deleteUploadById(1L);
    }

    @Test
    @DisplayName("不存在的记录直接跳过")
    void testDeleteUploadsSkipsMissingRecord()
    {
        when(uploadMapper.selectUploadById(1L)).thenReturn(null);

        assertEquals(0, uploadService.deleteUploads(new Long[] { 1L }));
        verify(uploadMapper, never()).deleteUploadById(any());
    }

    private Upload upload(Long id, String fileName)
    {
        Upload upload = new Upload();
        upload.setId(id);
        upload.setFileName(fileName);
        upload.setUrl("/profile/upload/2026/10/18/" + fileName);
        return upload;
    }

    private MediaReference reference(String type, Long id, String title)
    {
        MediaReference reference = new MediaReference();
        reference.setReferenceType(type);
        reference.setReferenceId(id);
        reference.setReferenceTitle(title);
        return reference;
    }
}
