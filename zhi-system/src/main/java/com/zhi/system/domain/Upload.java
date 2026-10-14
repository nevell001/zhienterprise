package com.zhi.system.domain;

/**
 * 博客上传记录对象
 *
 * @author nevell
 * @date 2026-09-05
 */
public class Upload
{
    private static final long serialVersionUID = 1L;

    private Long id;

    /** 存储文件名 */
    private String fileName;

    /** 原始文件名 */
    private String originalName;

    /** 访问路径 */
    private String url;

    /** MIME类型 */
    private String mimeType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 上传类型 */
    private String uploadType;

    /** 创建者 */
    private String createBy;

    private java.util.Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadType() {
        return uploadType;
    }

    public void setUploadType(String uploadType) {
        this.uploadType = uploadType;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }
}
