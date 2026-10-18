package com.zhi.system.service;

import java.util.List;
import com.zhi.system.domain.MediaReference;
import com.zhi.system.domain.Upload;

/**
 * 博客上传记录Service接口
 *
 * @author nevell
 * @date 2026-09-05
 */
public interface IUploadService
{
    /**
     * 记录一次上传
     *
     * @param file 上传记录字段（fileName/url/mimeType/fileSize/originalName/uploadType）
     * @return 结果
     */
    public int recordUpload(Upload file);

    /**
     * 查询上传记录列表
     *
     * @param upload 查询条件
     * @return 上传记录集合
     */
    public List<Upload> selectUploadList(Upload upload);

    /**
     * 查询上传文件被哪些内容引用
     *
     * @param id 上传记录ID
     * @return 引用位置集合（记录不存在时为空）
     */
    public List<MediaReference> selectReferences(Long id);

    /**
     * 批量删除上传记录（先校验引用，再删记录与物理文件）
     *
     * @param ids 记录ID集合
     * @return 删除的记录数
     */
    public int deleteUploads(Long[] ids);
}
