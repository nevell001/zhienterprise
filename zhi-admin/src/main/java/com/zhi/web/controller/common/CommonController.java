package com.zhi.web.controller.common;

import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.zhi.common.config.RuoYiConfig;
import com.zhi.common.annotation.RateLimiter;
import com.zhi.common.core.domain.AjaxResult;
import com.zhi.common.enums.LimitType;
import com.zhi.common.utils.StringUtils;
import com.zhi.common.utils.file.FileUploadUtils;
import com.zhi.common.utils.file.FileUtils;
import com.zhi.common.utils.file.MimeTypeUtils;
import com.zhi.framework.config.ServerConfig;
import com.zhi.system.domain.Upload;
import com.zhi.system.service.IUploadService;

/**
 * 通用请求处理
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/common")
public class CommonController
{
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    @Autowired
    private ServerConfig serverConfig;

    @Autowired
    private IUploadService uploadService;

    private static final String FILE_DELIMETER = ",";

    /**
     * 通用下载请求
     *
     * @param fileName 文件名称
     * @param delete 是否删除
     */
    @GetMapping("/download")
    public void fileDownload(@RequestParam String fileName, @RequestParam(required = false) Boolean delete, HttpServletResponse response, HttpServletRequest request)
    {
        try
        {
            if (!FileUtils.checkAllowDownload(fileName))
            {
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }
            String realFileName = System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);
            String filePath = RuoYiConfig.getDownloadPath() + fileName;

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (delete)
            {
                FileUtils.deleteFile(filePath);
            }
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 通用上传请求（单个）
     */
    @RateLimiter(key = "common:upload:", time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/upload")
    public AjaxResult uploadFile(MultipartFile file) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称
            String fileName = FileUploadUtils.upload(filePath, file);
            recordUpload(file, fileName, "upload");
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 通用上传请求（带图片压缩）
     */
    @RateLimiter(key = "common:upload:", time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/upload/compressed")
    public AjaxResult uploadFileCompressed(MultipartFile file) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称（带图片压缩）
            String fileName = FileUploadUtils.uploadWithCompression(filePath, file, MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION);
            recordUpload(file, fileName, "compressed");

            // 生成可访问的URL，使用相对路径避免域名问题
            String url = fileName; // 直接返回文件路径，前端会自动拼接域名

            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            ajax.put("compressed", true); // 标记是否进行了压缩
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 头像上传接口（专门压缩为头像尺寸）
     */
    @RateLimiter(key = "common:upload:", time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/upload/avatar")
    public AjaxResult uploadAvatar(MultipartFile file) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称（头像压缩）
            String fileName = FileUploadUtils.uploadAvatar(filePath, file, true);
            recordUpload(file, fileName, "avatar");

            // 生成可访问的URL，使用相对路径避免域名问题
            String url = fileName; // 直接返回文件路径

            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            ajax.put("type", "avatar"); // 标记为头像类型
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 缩略图上传接口
     */
    @RateLimiter(key = "common:upload:", time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/upload/thumbnail")
    public AjaxResult uploadThumbnail(MultipartFile file) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称（缩略图压缩）
            String fileName = FileUploadUtils.uploadThumbnail(filePath, file, true);
            recordUpload(file, fileName, "thumbnail");

            // 生成可访问的URL，使用相对路径避免域名问题
            String url = fileName; // 直接返回文件路径

            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            ajax.put("type", "thumbnail"); // 标记为缩略图类型
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 文章封面图上传接口
     */
    @RateLimiter(key = "common:upload:", time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/upload/article-cover")
    public AjaxResult uploadArticleCover(MultipartFile file) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称（文章封面压缩）
            String fileName = FileUploadUtils.uploadArticleCover(filePath, file, true);
            recordUpload(file, fileName, "article-cover");

            // 生成可访问的URL，使用相对路径避免域名问题
            String url = fileName; // 直接返回文件路径

            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            ajax.put("type", "article-cover"); // 标记为文章封面类型
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 移动端图片上传接口
     */
    @RateLimiter(key = "common:upload:", time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/upload/mobile")
    public AjaxResult uploadMobileImage(MultipartFile file) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称（移动端适配压缩）
            String fileName = FileUploadUtils.uploadMobileImage(filePath, file, true);
            recordUpload(file, fileName, "mobile");

            // 生成可访问的URL，使用相对路径避免域名问题
            String url = fileName; // 直接返回文件路径

            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            ajax.put("type", "mobile"); // 标记为移动端适配类型
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 带水印图片上传接口
     */
    @RateLimiter(key = "common:upload:", time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/upload/watermark")
    public AjaxResult uploadWatermarkImage(MultipartFile file, String watermarkText) throws Exception
    {
        try
        {
            if (StringUtils.isEmpty(watermarkText)) {
                watermarkText = "版权所有";
            }

            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            // 上传并返回新文件名称（添加水印）
            String fileName = FileUploadUtils.uploadWatermarkImage(filePath, file, watermarkText, true);
            recordUpload(file, fileName, "watermark");

            // 生成可访问的URL，使用相对路径避免域名问题
            String url = fileName; // 直接返回文件路径

            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", fileName);
            ajax.put("newFileName", FileUtils.getName(fileName));
            ajax.put("originalFilename", file.getOriginalFilename());
            ajax.put("watermarkText", watermarkText);
            ajax.put("type", "watermark"); // 标记为水印类型
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 通用上传请求（多个）
     */
    @RateLimiter(key = "common:upload:", time = 60, count = 20, limitType = LimitType.IP)
    @PostMapping("/uploads")
    public AjaxResult uploadFiles(List<MultipartFile> files) throws Exception
    {
        try
        {
            // 上传文件路径
            String filePath = RuoYiConfig.getUploadPath();
            List<String> urls = new ArrayList<String>();
            List<String> fileNames = new ArrayList<String>();
            List<String> newFileNames = new ArrayList<String>();
            List<String> originalFilenames = new ArrayList<String>();
            for (MultipartFile file : files)
            {
                // 上传并返回新文件名称
                String fileName = FileUploadUtils.upload(filePath, file);
                recordUpload(file, fileName, "uploads");
                String url = serverConfig.getUrl() + fileName;
                urls.add(url);
                fileNames.add(fileName);
                newFileNames.add(FileUtils.getName(fileName));
                originalFilenames.add(file.getOriginalFilename());
            }
            AjaxResult ajax = AjaxResult.success();
            ajax.put("urls", StringUtils.join(urls, FILE_DELIMETER));
            ajax.put("fileNames", StringUtils.join(fileNames, FILE_DELIMETER));
            ajax.put("newFileNames", StringUtils.join(newFileNames, FILE_DELIMETER));
            ajax.put("originalFilenames", StringUtils.join(originalFilenames, FILE_DELIMETER));
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 本地资源通用下载
     */
    @GetMapping("/download/resource")
    public void resourceDownload(@RequestParam String resource, HttpServletRequest request, HttpServletResponse response)
            throws Exception
    {
        try
        {
            if (!FileUtils.checkAllowDownload(resource))
            {
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            // 本地资源路径
            String localPath = RuoYiConfig.getProfile();
            // 数据库资源地址
            String downloadPath = localPath + FileUtils.stripPrefix(resource);
            // 下载名称
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        }
        catch (Exception e)
        {
            log.error("下载文件失败", e);
        }
    }

    /**
     * 记录上传到媒体库（上传成功后才调用）
     */
    private void recordUpload(MultipartFile file, String fileName, String uploadType)
    {
        try
        {
            Upload record = new Upload();
            record.setFileName(FileUtils.getName(fileName));
            record.setOriginalName(file.getOriginalFilename());
            record.setUrl(fileName);
            record.setMimeType(file.getContentType());
            record.setFileSize(file.getSize());
            record.setUploadType(uploadType);
            uploadService.recordUpload(record);
        }
        catch (Exception e)
        {
            log.error("记录上传信息失败", e);
        }
    }

}
