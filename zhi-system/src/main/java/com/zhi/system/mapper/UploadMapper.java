package com.zhi.system.mapper;

import java.util.List;
import com.zhi.system.domain.Upload;

/**
 * 博客上传记录Mapper接口
 *
 * @author nevell
 * @date 2026-09-05
 */
public interface UploadMapper
{
    public List<Upload> selectUploadList(Upload upload);

    public Upload selectUploadById(Long id);

    public int insertUpload(Upload upload);

    public int deleteUploadById(Long id);
}
