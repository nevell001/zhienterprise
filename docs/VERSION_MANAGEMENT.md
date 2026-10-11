# 版本管理指南

## 版本号统一管理机制

本项目采用统一的版本管理机制，确保所有地方的版本号保持一致。

### 版本号定义位置

**后端版本号**：
- **主配置文件**：`pom.xml` 中的 `<version>` 和 `<app.version>` 属性
- **当前版本**：`1.0.0`

**前端版本号**：
- **配置文件**：`zhi-ui/package.json` 中的 `version` 字段（`zhi-ui/package-lock.json` 的两处 `"version"` 一并同步）
- **当前版本**：`1.0.0`（与后端同步）

### 版本号使用位置

#### 后端

1. **Maven 主配置** (`pom.xml`)
   ```xml
   <groupId>top.nevell</groupId>
   <artifactId>zhiblog</artifactId>
   <version>1.0.0</version>  <!-- 项目版本 -->
   
   <properties>
       <app.version>1.0.0</app.version>  <!-- 应用版本 -->
   </properties>
   ```

2. **应用配置** (`zhi-admin/src/main/resources/application.yml`)
   ```yaml
   ruoyi:
     version: @app.version@  # Maven 资源过滤会替换为实际版本号
   ```

3. **生产环境配置** (`zhi-admin/src/main/resources/application-prod.yml`)
   ```yaml
   ruoyi:
     version: @app.version@  # 必须使用 Maven 占位符，不能硬编码
   ```
   **重要说明**：生产环境配置文件必须使用 `@app.version@` 占位符，不能硬编码版本号，否则会在生产环境中显示错误的版本号。

4. **配置类** (`zhi-common/src/main/java/com/zhi/common/config/RuoYiConfig.java`)
   ```java
   @Value("${ruoyi.version:1.0.0}")
   private String version;
   ```

5. **API 接口** (`SysIndexController.java`)
   - 接口路径：`GET /system/version`
   - 返回数据：`{ "version": "1.0.0", "name": "知企建站系统" }`

6. **子模块 parent 版本**：
   - `zhi-common/pom.xml`
   - `zhi-system/pom.xml`
   - `zhi-framework/pom.xml`
   - `zhi-quartz/pom.xml`
   - `zhi-generator/pom.xml`
   - `zhi-admin/pom.xml`
   ```xml
   <parent>
       <groupId>top.nevell</groupId>
       <artifactId>zhiblog</artifactId>
       <version>1.0.0</version>  <!-- 必须与父 POM 版本一致 -->
   </parent>
   ```

#### 前端

前端不再硬编码版本号：

1. **package.json** (`zhi-ui/package.json`)
   - `"version": "1.0.0"` 必须与后端保持一致（仅用于包元信息）。
2. **lock 文件** (`zhi-ui/package-lock.json`)
   - 根节点的 `"version"` 与 `packages[""].version` 两处都要跟着 `package.json` 改。
3. **界面展示**
   - 前端没有静态版本展示页；如需展示版本，调用后端接口 `GET /system/version` 获取
     （返回值来自 `@app.version@` 资源过滤，是唯一可信来源）。

### 如何更新版本号

#### 更新后端版本号

**需要修改多个地方**（重要！）：

1. **修改父 POM** (`pom.xml`)
   - 第 9 行：`<version>1.0.0</version>` → `<version>1.0.1</version>`
   - 第 24 行：`<app.version>1.0.0</app.version>` → `<app.version>1.0.1</app.version>`

2. **修改所有子模块的 parent 版本**：
   - `zhi-common/pom.xml`
   - `zhi-system/pom.xml`
   - `zhi-framework/pom.xml`
   - `zhi-quartz/pom.xml`
   - `zhi-generator/pom.xml`
   - `zhi-admin/pom.xml`
   
   每个文件中的：
   ```xml
   <version>1.0.0</version>  →  <version>1.0.1</version>
   ```

3. **修改配置类兜底值**（`zhi-common/src/main/java/com/zhi/common/config/RuoYiConfig.java`）：
   `@Value("${ruoyi.version:1.0.0}")` 的默认值一并跟上，否则哪天 `ruoyi.version` 没注入成功，
   `/system/version` 会静默回落到旧版本号。

4. **重新编译项目**：
   ```bash
   mvn clean install -DskipTests
   ```

5. **重启后端服务**

**自动同步**：
- Maven 构建时会自动将 `application.yml` 中的 `@app.version@` 替换为实际版本号
- 其他地方会从配置文件或 API 中读取最新版本号

#### 批量更新脚本

为了简化版本号更新，可以使用以下命令批量更新（以 `1.0.0` → `1.0.1` 为例；macOS/BSD 的 `sed -i` 必须带空后缀 `''`，GNU/Linux 上写成 `sed -i` 即可）：

```bash
OLD=1.0.0 NEW=1.0.1

# 1. 父 POM：第 9 行 <version> 与第 24 行 <app.version>
sed -i '' -e "s|<version>${OLD}</version>|<version>${NEW}</version>|" \
          -e "s|<app.version>${OLD}</app.version>|<app.version>${NEW}</app.version>|" pom.xml

# 2. 所有子模块的 parent 版本（各自第 8 行）
for m in zhi-common zhi-system zhi-framework zhi-quartz zhi-generator zhi-admin; do
    sed -i '' "8s|<version>${OLD}</version>|<version>${NEW}</version>|" $m/pom.xml
done

# 3. 配置类兜底值
sed -i '' "s|ruoyi.version:${OLD}|ruoyi.version:${NEW}|" \
    zhi-common/src/main/java/com/zhi/common/config/RuoYiConfig.java

# 4. 前端 package.json + package-lock.json（lock 有两处：根节点与 packages[""]）
sed -i '' "3s|\"version\": \"${OLD}\",|\"version\": \"${NEW}\",|" zhi-ui/package.json
sed -i '' -e "3s|\"version\": \"${OLD}\",|\"version\": \"${NEW}\",|" \
          -e "9s|\"version\": \"${OLD}\",|\"version\": \"${NEW}\",|" zhi-ui/package-lock.json

# 5. 重新编译
mvn clean install -DskipTests
```

改完自查（`${OLD}([^0-9]|$)` 是为了不被依赖树里前缀相同的版本号噪声淹没；正常只会剩 `docs/releases/` 发行说明与本文脚本自身的示例）：

```bash
grep -rEn "${OLD}([^0-9]|$)" --include='*.xml' --include='*.java' --include='*.json' --include='*.md' \
    --exclude-dir=node_modules --exclude-dir=target .
```

#### 更新前端版本号

前端版本号不独立演进，始终与后端保持一致：

1. 打开 `zhi-ui/package.json`，修改 `version` 字段
2. 同步 `zhi-ui/package-lock.json` 里本包的两处 `"version"`：根节点（第 3 行）与 `packages[""]`（第 9 行）；其余 `"version"` 属于第三方依赖，不要动
3. 界面不再静态展示版本：需要显示时调 `GET /system/version`

> 前端版本只是包元信息，运行时版本号唯一可信来源仍是后端 `@app.version@`。

### 版本号规范

遵循 [语义化版本](https://semver.org/lang/zh-CN/) 规范：

- **主版本号（MAJOR）**：不兼容的 API 修改
- **次版本号（MINOR）**：向下兼容的功能性新增
- **修订号（PATCH）**：向下兼容的问题修正

示例：`1.0.0`
- `1`：主版本号（MAJOR）
- `0`：次版本号（MINOR）
- `0`：修订号（PATCH）

### 版本号检查

#### 后端版本号检查

1. **查看父 POM 配置**：
   ```bash
   grep -E "(<version>|<app.version>)" pom.xml | head -3
   ```

2. **查看子模块 parent 版本**：
   ```bash
   for module in zhi-common zhi-system zhi-framework zhi-quartz zhi-generator zhi-admin; do
       echo "=== $module ==="
       grep -A 5 "<parent>" $module/pom.xml | grep -E "(groupId|artifactId|version)"
   done
   ```

3. **查看编译后的配置**：
   ```bash
   grep "version:" zhi-admin/target/classes/application.yml | head -1
   ```

4. **通过 API 检查**：
   ```bash
   curl http://localhost:8080/system/version
   ```

#### 前端版本号检查

1. **查看 package.json**：
   ```bash
   grep "version" zhi-ui/package.json
   ```

2. **查看管理后台首页**：
   - 访问：http://localhost:3000/admin
   - 查看"系统状态"卡片中的"系统版本"

### 注意事项

1. **版本号一致性**：
   - 父 POM 的 `<version>` 和 `<app.version>` 必须一致
   - 所有子模块的 parent version 必须与父 POM 的 version 一致
   - 不要直接修改 `application.yml` 中的版本号（会被 Maven 覆盖）
   - 不要直接修改代码中的版本号（应该从配置读取）

2. **Maven 资源过滤**：
   - `zhi-admin/pom.xml` 已配置资源过滤
   - `application.yml` 中的 `@app.version@` 会在构建时自动替换

3. **前端获取版本号**：
   - 前端通过 API `/system/version` 获取后端版本号
   - 如果 API 调用失败，使用前端默认值

4. **版本号更新后**：
   - 必须同时修改父 POM 和所有子模块的版本号
   - 必须重新编译后端项目
   - 必须重启后端服务
   - 前端会自动获取最新版本号

5. **版本不匹配的错误**：
   - 如果父 POM 版本与子模块 parent 版本不一致，构建会失败
   - 错误信息：`Non-resolvable parent POM`
   - 解决方法：确保所有版本号一致

## 发布流程（tag 与 Release）

版本号改完、代码推送之后，**还必须单独打标签**，否则 GitHub/Gitee 的 Releases 页会一直停留在旧版本。

### 1. 准备发行说明

在 `docs/releases/<版本>.md` 写发行说明（建议结构：安全与隐私 / 新增与增强 / 主要修复 / 升级提示；只写功能与 bug 修复，不写测试、覆盖率、CI 流水线等工程细节）。

同时把 README 的「项目信息」以及 `AGENTS.md` / `CLAUDE.md` 的 `Current version` 一起对齐。

> **先改版本号再打 tag**：`.workflow/ReleasePipeline.yml` 在 backend 阶段比对「tag 去 `v` 后的版本」与根 `pom.xml` 的 `<version>`，不一致直接 `exit 1`。
> 该流水线的 `release@gitee` 步骤里 `releaseName` / `description` / `tagName` 与 `triggers.push.tags.exclude` 也写死了版本号，发新版时一并改。

### 2. 推送代码（含标签）

```bash
./sync-github.sh          # 已用 --follow-tags，会一并推送 main 上可达的注释标签
```

### 3. 打注释标签并推送（首次发布新版本时执行）

```bash
git tag -a v1.0.1 -F docs/releases/v1.0.1.md     # 标签消息 = 发行说明
git push origin v1.0.1                            # Gitee
git push github v1.0.1                            # GitHub
```

### 4. Release 由 CI 自动创建（推送 tag 之后只需校验）

**两个平台的 tag 推送都会自动建 Release，不需要手动新建**：

- **GitHub**：`.github/workflows/release.yml` 在 `v*` tag 推送后跑 `mvn clean verify` + 前端全套门禁，
  成功后用 `gh release create` 建 Release，**发行说明直接取 `docs/releases/<tag>.md`**
  （找不到该文件才回落到自动生成的说明文字），并附三个产物：`zhi-admin.jar`、`frontend-dist.zip`、`deploy-assets.zip`。
  同 tag 重复发布时它会先 `gh release delete --yes` 再重建，所以**不要在网页上手改**这个 Release——重跑流水线会覆盖。
- **Gitee**：`.workflow/ReleasePipeline.yml` 的 `release@gitee` 步骤同理，
  但它读的是文件里写死的 `releaseName` / `description` / `tagName`，**每次发版要手工改版本号**（见 `.workflow/README.md`）。

校验（两边都应有该 tag 的 Release，且 workflow 已跑完）：

```bash
gh run list -R nevell001/zhienterprise --workflow release.yml --limit 1
gh release view v1.0.1 -R nevell001/zhienterprise \
  --json name,isDraft,assets -q '"\(.name) draft=\(.isDraft) 附件=\([.assets[].name] | join(", "))"'
git ls-remote --tags github "refs/tags/v1.0.1"       # tag 对象 SHA 应为 v1.0.1 的 tag，不是裸 commit
```

### 5. 校验

```bash
git ls-remote --tags origin    # Gitee 上应有的 tag
git ls-remote --tags github    # GitHub 上应有的 tag
```

> **常见坑**：`git push origin main` **不会**推送标签（`git push` 默认不带 tag），只推 main 会漏掉 tag。
> Releases 页仍是旧版本时，先查上一步的两个 `ls-remote` 输出里有没有新 tag。

### 常见问题

**Q: 为什么需要同时修改父 POM 和子模块的版本号？**

A: 因为子模块通过 `<parent>` 标签引用父 POM，版本号必须一致才能正常构建。

**Q: 为什么 application.yml 中的版本号是 `@app.version@`？**

A: 这是 Maven 资源过滤的占位符，在构建时会自动替换为 `pom.xml` 中定义的实际版本号。

**Q: 为什么前端版本号和后端版本号不一样？**

A: 不应该不一样。前端 `package.json`（及 `package-lock.json`）的版本号与后端保持一致（当前均为 1.0.0），仅作包元信息；界面要展示版本时读 `GET /system/version`。

**Q: 如何确保所有地方的版本号一致？**

A: 需要同时修改：
1. 父 POM 的 `<version>` 和 `<app.version>`
2. 所有子模块的 parent `<version>`
3. `RuoYiConfig` 的 `${ruoyi.version:}` 兜底值
4. `zhi-ui/package.json` 与 `package-lock.json`
5. 文档：README、`AGENTS.md`、`CLAUDE.md`，以及 `.workflow/ReleasePipeline.yml`
6. 然后运行 `mvn clean install`

**Q: 版本号更新后需要做什么？**

A:
1. 修改 `pom.xml` 中的 `<version>` 和 `<app.version>`
2. 修改所有子模块的 parent `<version>`
3. 同步 `RuoYiConfig` 兜底值、前端 `package.json` / `package-lock.json` 与上述文档
4. 运行 `mvn clean install -DskipTests`
5. 重启后端服务（或重建镜像）
6. 前端会自动获取最新版本号

**Q: 构建时提示 "Non-resolvable parent POM" 怎么办？**

A: 这说明父 POM 版本与子模块 parent 版本不一致。检查并确保所有版本号一致。

---

**文档版本**: v1.0
**最后更新**: 2026-10-11
**维护者**: nevell
