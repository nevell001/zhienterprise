<template>
  <div class="errPage-container">
    <el-button icon="arrow-left" class="back-btn" @click="back">返回上一页</el-button>
    <el-row justify="center" align="middle">
      <el-col :span="12">
        <h1 class="text-jumbo">401</h1>
        <h2>需要权限访问</h2>
        <p>您目前没有足够的权限访问此页面</p>
        <div class="action-buttons">
          <router-link to="/" class="home-link">返回首页</router-link>
        </div>
      </el-col>
      <el-col :span="12" class="image-container">
        <img :src="errGif" alt="权限不足提示" class="error-image" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import errImage from '@/assets/401_images/401.gif'

const { proxy } = getCurrentInstance()

const errGif = ref(errImage + '?' + +new Date())

function back() {
  if (proxy.$route.query.noGoBack) {
    proxy.$router.push({ path: '/' })
  } else {
    proxy.$router.go(-1)
  }
}
</script>

<style lang="scss" scoped>
.errPage-container {
  width: 800px;
  max-width: 100%;
  margin: 60px auto;

  .back-btn {
    background: var(--el-color-primary);
    color: #fff;
    border: none;
    margin-bottom: 20px;
  }

  .image-container {
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .error-image {
    max-width: 100%;
    height: auto;
    max-height: 300px;
  }

  .text-jumbo {
    font-size: 72px;
    font-weight: 700;
    color: var(--el-color-primary);
    margin-bottom: 10px;
  }

  h2 {
    color: var(--el-text-color-regular);
    margin-bottom: 15px;
    font-size: 24px;
  }

  p {
    color: var(--el-text-color-secondary);
    margin-bottom: 20px;
    font-size: 16px;
  }

  .action-buttons {
    margin-top: 20px;
  }

  .home-link {
    display: inline-block;
    padding: 8px 20px;
    background: var(--el-color-primary);
    color: #fff;
    text-decoration: none;
    border-radius: 4px;
    transition: background-color 0.3s;

    &:hover {
      background: var(--el-color-primary-light-3);
      text-decoration: none;
    }
  }
}
</style>
