#!/bin/bash
# 同步代码到 GitHub 脚本

set -e

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}    同步代码到 GitHub${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# GitHub 仓库地址（请修改为你的仓库地址）
GITHUB_REPO="https://github.com/nevell001/zhienterprise.git"
GITHUB_REPO_SSH="git@github.com:nevell001/zhienterprise.git"

# 检查是否已添加 github 远端
if git remote | grep -q "^github$"; then
    echo -e "${GREEN}✓ GitHub 远端已存在${NC}"
    current_url="$(git remote get-url github)"
    current_pushurl="$(git remote get-url --push github)"
    case "$current_url" in
        git@*|ssh://*)
            # 已经改成 SSH 了（常见于 https 直连不稳或全局配了 GitHub 加速代理的情况），不要覆盖
            echo -e "${GREEN}  保留已配置的 SSH 地址：$current_url${NC}"
            ;;
        *)
            # 只写 https 的 fetch 地址；pushurl 单独配置时（例如 SSH 推送）不会被影响
            git remote set-url github "$GITHUB_REPO"
            ;;
    esac
    case "$current_pushurl" in
        *ghproxy*|*ghfast*|*gh-proxy*)
            echo -e "${YELLOW}  提示：推送地址走了第三方加速代理（$current_pushurl），建议改用 SSH：${NC}"
            echo -e "${YELLOW}        git remote set-url --push github $GITHUB_REPO_SSH${NC}"
            ;;
    esac
else
    echo -e "${YELLOW}添加 GitHub 远端...${NC}"
    git remote add github "$GITHUB_REPO"
    echo -e "${GREEN}✓ GitHub 远端已添加${NC}"
fi

echo ""
echo -e "${BLUE}当前远端配置：${NC}"
git remote -v

echo ""
echo -e "${YELLOW}正在推送到 Gitee (origin)...${NC}"
# --follow-tags：一并推送 main 上可达的注释标签（普通 git push 不会带标签）
git push origin main --follow-tags

echo ""
echo -e "${YELLOW}正在推送到 GitHub (github)...${NC}"
if ! git push github main --follow-tags; then
    echo ""
    echo -e "${RED}✗ GitHub 推送失败${NC}"
    echo -e "${YELLOW}常见原因与处理：${NC}"
    echo -e "  1) https 直连不稳 / 全局 gitconfig 把 github.com 重写成 ghproxy 等加速站，"
    echo -e "     而加速站不接受推送 → 改用 SSH：git remote set-url --push github $GITHUB_REPO_SSH"
    echo -e "  2) SSH 未生效 → 先验证：ssh -T git@github.com（应显示 Hi <用户名>!）"
    echo -e "  3) 临时绕过重写推送：GIT_CONFIG_GLOBAL=<(grep -v insteadOf ~/.gitconfig) git push github main"
    echo -e "  4) pushurl 指向 SSH 但本机没有可用密钥时，可直接用显式 URL 走 https 直连"
    echo -e "     （注意：-c remote.github.pushurl=... 无法覆盖已存在的 pushurl，必须显式给 URL）："
    echo -e "     GIT_CONFIG_GLOBAL=<(grep -v insteadOf ~/.gitconfig) \\"
    echo -e "       git -c http.version=HTTP/1.1 push https://github.com/nevell001/zhienterprise.git main --follow-tags"
    exit 1
fi

echo ""
echo -e "${BLUE}已推送的标签：${NC}"
git ls-remote --tags origin | sed 's/^/  origin  /' | grep -v '\^{}' || true
git ls-remote --tags github | sed 's/^/  github  /' | grep -v '\^{}' || true

echo ""
echo -e "${YELLOW}提示：推送 tag 后 GitHub/Gitee 的 CI 会自动创建发行版，无需手动新建${NC}"
echo -e "${YELLOW}      注意：tag 请逐个推送——单次超过 3 个 tag 时 GitHub 不会触发发布流水线${NC}"
echo -e "${YELLOW}      （说明见 docs/VERSION_MANAGEMENT.md 的「发布流程」）${NC}"

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}    ✓ 同步完成！${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo -e "Gitee:  ${BLUE}https://gitee.com/nevell/zhienterprise${NC}"
echo -e "GitHub: ${BLUE}https://github.com/nevell001/zhienterprise${NC}"
