package com.zhi.system.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhi.common.config.RuoYiConfig;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.common.utils.file.FileUtils;
import com.zhi.system.domain.Upload;
import com.zhi.system.mapper.UploadMapper;
import com.zhi.system.service.IUploadService;

/**
 * 博客上传记录Service实现
 *
 * @author nevell
 * @date 2026-09-05
 */
@Service
public class UploadServiceImpl implements IUploadService
{
    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);

    @Autowired
    private UploadMapper uploadMapper;

    @Override
    public int recordUpload(Upload file)
    {
        if (file == null || StringUtils.isEmpty(file.getFileName()))
        {
            return 0;
        }
        try
        {
            file.setCreateBy(SecurityUtils.getUsername());
        }
        catch (Exception e)
        {
            // 匿名场景下取不到用户名，忽略
            file.setCreateBy("anonymous");
        }
        if (file.getFileSize() == null)
        {
            file.setFileSize(0L);
        }
        if (StringUtils.isEmpty(file.getUploadType()))
        {
            file.setUploadType("upload");
        }
        return uploadMapper.insertUpload(file);
    }

    @Override
    public List<Upload> selectUploadList(Upload upload)
    {
        return uploadMapper.selectUploadList(upload);
    }

    @Override
    public int deleteUploadById(Long id)
    {
        Upload record = uploadMapper.selectUploadById(id);
        if (record == null)
        {
            return 0;
        }
        int result = uploadMapper.deleteUploadById(id);
        if (result > 0 && StringUtils.isNotEmpty(record.getUrl()))
        {
            try
            {
                // 尝试删除物理文件：url 形如 /profile/upload/xxx
                String localPath = RuoYiConfig.getProfile();
                String filePath = localPath + FileUtils.stripPrefix(record.getUrl());
                FileUtils.deleteFile(filePath);
            }
            catch (Exception e)
            {
                log.warn("删除上传物理文件失败: url={}, error={}", record.getUrl(), e.getMessage());
            }
        }
        return result;
    }
}
