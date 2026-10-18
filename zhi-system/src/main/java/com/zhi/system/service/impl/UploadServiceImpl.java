package com.zhi.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zhi.common.config.RuoYiConfig;
import com.zhi.common.exception.ServiceException;
import com.zhi.common.utils.SecurityUtils;
import com.zhi.common.utils.StringUtils;
import com.zhi.common.utils.file.FileUtils;
import com.zhi.system.domain.MediaReference;
import com.zhi.system.domain.Upload;
import com.zhi.system.mapper.ArticleMapper;
import com.zhi.system.mapper.FriendLinkMapper;
import com.zhi.system.mapper.PageMapper;
import com.zhi.system.mapper.SettingMapper;
import com.zhi.system.mapper.SysUserMapper;
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

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private PageMapper pageMapper;

    @Autowired
    private SettingMapper settingMapper;

    @Autowired
    private FriendLinkMapper friendLinkMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

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
    public List<MediaReference> selectReferences(Long id)
    {
        Upload record = uploadMapper.selectUploadById(id);
        if (record == null)
        {
            return new ArrayList<>();
        }
        return selectReferences(record);
    }

    @Override
    @Transactional
    public int deleteUploads(Long[] ids)
    {
        if (ids == null || ids.length == 0)
        {
            return 0;
        }
        List<Upload> records = new ArrayList<>();
        List<String> blocked = new ArrayList<>();
        for (Long id : ids)
        {
            Upload record = uploadMapper.selectUploadById(id);
            if (record == null)
            {
                continue;
            }
            List<MediaReference> references = selectReferences(record);
            if (references.isEmpty())
            {
                records.add(record);
            }
            else
            {
                blocked.add("「" + record.getFileName() + "」被" + describe(references) + "引用");
            }
        }
        if (!blocked.isEmpty())
        {
            // 先全量校验再删除，避免一批里删掉一半、另一半被引用而中断
            throw new ServiceException("以下文件正在被引用，无法删除：" + String.join("；", blocked));
        }

        int total = 0;
        for (Upload record : records)
        {
            total += uploadMapper.deleteUploadById(record.getId());
        }
        // 物理文件放在数据库删除全部成功之后，避免事务回滚时文件已经删掉
        for (Upload record : records)
        {
            deletePhysicalFile(record);
        }
        return total;
    }

    private List<MediaReference> selectReferences(Upload record)
    {
        String keyword = StringUtils.isNotEmpty(record.getFileName())
            ? record.getFileName() : FileUtils.getName(record.getUrl());
        if (StringUtils.isEmpty(keyword))
        {
            return new ArrayList<>();
        }
        List<MediaReference> references = new ArrayList<>();
        references.addAll(articleMapper.selectMediaReferences(keyword));
        references.addAll(pageMapper.selectMediaReferences(keyword));
        references.addAll(settingMapper.selectMediaReferences(keyword));
        references.addAll(friendLinkMapper.selectMediaReferences(keyword));
        references.addAll(sysUserMapper.selectMediaReferences(keyword));
        return references;
    }

    private String describe(List<MediaReference> references)
    {
        String summary = references.stream()
            .limit(3)
            .map(reference -> typeLabel(reference.getReferenceType()) + "「"
                + reference.getReferenceTitle() + "」")
            .collect(Collectors.joining("、"));
        return references.size() > 3 ? summary + "等 " + references.size() + " 处" : summary;
    }

    private String typeLabel(String type)
    {
        if ("article".equals(type))
        {
            return "内容";
        }
        if ("page".equals(type))
        {
            return "页面";
        }
        if ("setting".equals(type))
        {
            return "站点设置";
        }
        if ("friend_link".equals(type))
        {
            return "友链";
        }
        if ("user".equals(type))
        {
            return "用户头像";
        }
        return "其他";
    }

    private void deletePhysicalFile(Upload record)
    {
        if (StringUtils.isEmpty(record.getUrl()))
        {
            return;
        }
        try
        {
            // url 形如 /profile/upload/xxx
            String filePath = RuoYiConfig.getProfile() + FileUtils.stripPrefix(record.getUrl());
            FileUtils.deleteFile(filePath);
        }
        catch (Exception e)
        {
            log.warn("删除上传物理文件失败: url={}, error={}", record.getUrl(), e.getMessage());
        }
    }
}
