# 安全配置验证功能说明

## 概述

知企建站系统内置自动安全配置验证功能，在应用启动时自动检查关键安全配置，防止使用不安全的默认值。

## 验证项

系统会自动验证以下安全配置：

### 1. JWT 密钥 (R_TOKEN_SECRET)
- **要求**：必须设置且长度至少 64 位
- **生成命令**：`openssl rand -base64 64`
- **严重性**：生产环境未设置将阻止应用启动

### 2. Druid 监控密码 (DRUID_PASSWORD)
- **要求**：必须设置非空密码
- **生成命令**：`openssl rand -base64 32`
- **严重性**：生产环境未设置将阻止应用启动

### 3. Redis 密码 (REDIS_PASSWORD)
- **要求**：必须设置非空密码
- **生成命令**：`openssl rand -base64 32`
- **严重性**：生产环境未设置将阻止应用启动

### 4. 数据库密码 (DB_PASSWORD / DB_ROOT_PASSWORD)
- **要求**：`DB_PASSWORD` 为应用账号密码，必须设置且不能是弱密码（root, password, 123456 等）
- **要求**：`DB_ROOT_PASSWORD` 为 MySQL root 初始化密码，仅用于容器初始化和健康检查，生产环境必须单独设置
- **生成命令**：`openssl rand -base64 32`
- **严重性**：未设置或使用弱密码将阻止应用启动（所有环境）

## 环境差异

### 开发环境 (dev)
- 安全配置缺失会显示 **警告** 日志
- 应用可以正常启动
- 建议设置所有密码以确保安全

### 生产环境 (prod/production)
- 安全配置缺失会显示 **错误** 日志
- **应用将无法启动**，直到所有必填项都正确配置
- 强制执行安全最佳实践

## 配置方式

### 方法 1：使用 .env 文件（推荐）

1. 复制 `.env.example` 为 `.env`：
   ```bash
   cp .env.example .env
   ```

2. 编辑 `.env` 文件，设置所有必填的环境变量：
   ```bash
   # JWT 密钥（至少 64 位）
   R_TOKEN_SECRET=$(openssl rand -base64 64)

   # Druid 监控密码
   DRUID_PASSWORD=$(openssl rand -base64 32)

   # Redis 密码
   REDIS_PASSWORD=$(openssl rand -base64 32)

   # 数据库密码
   DB_PASSWORD=$(openssl rand -base64 32)
   ```

3. 确保 `.env` 文件不会被提交到版本控制系统（已在 `.gitignore` 中）

### 方法 2：直接设置环境变量

```bash
export R_TOKEN_SECRET=$(openssl rand -base64 64)
export DRUID_PASSWORD=$(openssl rand -base64 32)
export REDIS_PASSWORD=$(openssl rand -base64 32)
export DB_PASSWORD=$(openssl rand -base64 32)
```

### 方法 3：在 Docker Compose 中配置

在 `docker-compose.dev.yml` 或 `docker-compose.prod.yml` 中使用 `env_file`：

```yaml
services:
  zhi-admin:
    env_file:
      - .env
```

## 禁用验证（仅限开发环境）

⚠️ **警告**：生产环境无法禁用安全验证！

在开发环境中，如需临时禁用验证：

```bash
export SECURITY_VALIDATION_ENABLED=false
```

或在 `.env` 文件中：
```bash
SECURITY_VALIDATION_ENABLED=false
```

## 启动日志示例

### ✅ 成功启动（所有配置正确）

```
🔒 开始验证安全配置...
📍 当前环境: prod
✅ JWT密钥：已设置且长度符合要求（64 字符）
✅ Druid监控密码：已设置
✅ Redis密码：已设置
✅ 数据库密码：已设置
🔒 安全配置验证完成
```

### ⚠️ 开发环境警告（配置缺失）

```
🔒 开始验证安全配置...
📍 当前环境: dev
⚠️  JWT密钥未设置（开发环境）
   建议设置环境变量 R_TOKEN_SECRET
   生成命令：openssl rand -base64 64
⚠️  Redis密码未设置（开发环境）
   建议设置环境变量 REDIS_PASSWORD
   生成命令：openssl rand -base64 32
✅ 数据库密码：已设置
🔒 安全配置验证完成
```

### ❌ 生产环境错误（应用启动失败）

```
🔒 开始验证安全配置...
📍 当前环境: prod
❌ 安全错误：JWT密钥未设置！
   解决方法：设置环境变量 R_TOKEN_SECRET
   生成命令：openssl rand -base64 64
❌ 安全错误：Druid监控密码未设置！
   解决方法：设置环境变量 DRUID_PASSWORD
   生成命令：openssl rand -base64 32
🔒 安全配置验证完成
🛑 生产环境安全配置验证失败！应用启动已阻止。请检查上述错误并设置正确的环境变量。
```

## 故障排查

### 问题：应用启动失败，提示安全配置验证失败

**解决方法**：
1. 检查当前环境：`echo $SPRING_PROFILES_ACTIVE`
2. 如果是生产环境，必须设置所有必填密码
3. 如果是开发环境且想跳过验证，设置 `SECURITY_VALIDATION_ENABLED=false`
4. 重新启动应用

### 问题：设置了环境变量但仍然报错

**解决方法**：
1. 确认环境变量已正确设置：`echo $R_TOKEN_SECRET`
2. 检查变量名拼写是否正确
3. 确认 `.env` 文件在正确的位置
4. 如果使用 Docker，确认 `env_file` 配置正确

### 问题：密码长度不足

**解决方法**：
1. JWT 密钥必须至少 64 位字符
2. 其他密码建议至少 32 位字符
3. 使用提供的生成命令确保足够的长度

## 安全最佳实践

1. **定期轮换密钥**：建议每 3-6 个月更换一次密钥和密码
2. **使用强密码**：至少 32 位，包含大小写字母、数字和特殊字符
3. **保护密钥**：不要将密钥提交到版本控制系统
4. **使用密钥管理服务**：生产环境建议使用 AWS Secrets Manager、Azure Key Vault 等
5. **最小权限原则**：数据库用户只授予必要的权限
6. **启用审计**：记录所有敏感操作的日志

## 相关文件

- **配置验证器**：`zhi-framework/src/main/java/com/zhi/framework/config/SecurityConfigValidator.java`
- **异常类**：`zhi-common/src/main/java/com/zhi/common/exception/SecurityConfigValidationException.java`
- **配置文件**：`zhi-admin/src/main/resources/application.yml`
- **环境变量模板**：`.env.example`

## 技术细节

### 实现原理

1. 使用 Spring `@Configuration` 和 `@PostConstruct` 注解
2. 在应用启动时、Bean 初始化后自动执行验证
3. 通过 `@Value` 注入环境变量
4. 根据激活的配置文件（dev/prod）决定严格程度

### 兼容性

- 支持 Spring Boot 3.x
- 使用 Jakarta EE 9+ 命名空间（`jakarta.annotation.PostConstruct`）
- 兼容所有配置方式（环境变量、.env 文件、Docker secrets）

## 反馈与支持

如有问题或建议，请提交 Issue 或 Pull Request。
