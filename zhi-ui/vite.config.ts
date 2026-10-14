import { defineConfig, loadEnv } from 'vite'
import path from 'path'
import createVitePlugins from './vite/plugins'
import type { UserConfig, ConfigEnv } from 'vite'
import { visualizer } from 'rollup-plugin-visualizer'
import { devOptimizeDeps } from './vite/optimizeDeps'

// 判断是否在 Docker 容器内运行
// 通过环境变量 DOCKER 来控制（docker-compose.dev.yml / docker-compose.prod.yml 中设置）
const inDocker = process.env.DOCKER === 'true'

// 如果在容器内 → 用 zhi-admin 访问同网络下的后端服务
// 如果在本机开发 → 用 localhost 访问后端
const baseUrl = inDocker ? 'http://zhi-admin:8080' : 'http://localhost:8080'
const analyzePlugins =
  process.env.ANALYZE === 'true'
    ? [
        visualizer({
          open: false,
          filename: 'stats.html',
          gzipSize: true,
          brotliSize: true
        })
      ]
    : []

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }: ConfigEnv): UserConfig => {
  const env = loadEnv(mode, process.cwd())
  const { VITE_APP_ENV } = env

  return {
    // 部署生产环境和开发环境下的URL
    base: VITE_APP_ENV === 'production' ? '/' : '/',
    plugins: createVitePlugins(env),
    esbuild: {
      target: 'es2020',
      logLevel: 'error',
      legalComments: 'none'
    },
    resolve: {
      // 设置路径别名
      alias: {
        '~': path.resolve(__dirname, './'),
        '@': path.resolve(__dirname, './src')
      },
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    build: {
      sourcemap: command === 'build' ? false : 'inline',
      outDir: 'dist',
      assetsDir: 'assets',
      chunkSizeWarningLimit: 1200,
      rollupOptions: {
        plugins: [...analyzePlugins],
        output: {
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]',
          // 手动代码分割配置
          manualChunks: {
            // 将大型第三方库拆分为独立 chunks
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
            'element-icons': ['@element-plus/icons-vue'],
            echarts: ['echarts'],
            // 将通用工具库拆分为独立 chunk
            utils: ['axios', 'js-cookie', 'file-saver', 'fuse.js', '@vueuse/core']
          }
        }
      }
    },
    server: {
      host: '0.0.0.0', // 绑定到所有网络接口
      port: 3000,
      open: false, // 不自动打开浏览器，在容器中会导致错误
      // 启用 HMR，配合稳定的 optimizeDeps 清单避免开发态反复 reload
      hmr: true,
      // Docker bind mount（macOS/Windows）下宿主机改动往往收不到 inotify 事件，dev server 会
      // 持续返回旧的模块转换结果（表现为“改了代码页面没变化”），因此容器内改用轮询监听。
      ...(inDocker ? { watch: { usePolling: true, interval: 300 } } : {}),
      proxy: {
        // 接口代理 - 后端 API 前缀
        '/dev-api': {
          target: baseUrl,
          changeOrigin: true,
          // 转发真实客户端 IP，后端限流/冷却按访客生效（容器网络下直连方是内网代理）
          xfwd: true,
          rewrite: path => path.replace(/^\/dev-api/, '')
        },
        // 站点公开接口（/api/*）：原样转发，不做前缀改写
        '^/api/': {
          target: baseUrl,
          changeOrigin: true,
          // 转发真实客户端 IP，后端限流/冷却按访客生效（容器网络下直连方是内网代理）
          xfwd: true
        },
        // 代理系统管理接口
        '/system': {
          target: baseUrl,
          changeOrigin: true,
          // 转发真实客户端 IP，后端限流/冷却按访客生效（容器网络下直连方是内网代理）
          xfwd: true
        },
        // 代理通用接口（包括头像上传）
        '/common': {
          target: baseUrl,
          changeOrigin: true,
          // 转发真实客户端 IP，后端限流/冷却按访客生效（容器网络下直连方是内网代理）
          xfwd: true
        },
        // 代理静态资源访问（上传的图片、头像等）
        '/profile': {
          target: baseUrl,
          changeOrigin: true,
          // 转发真实客户端 IP，后端限流/冷却按访客生效（容器网络下直连方是内网代理）
          xfwd: true
        },
        // 代理 Actuator 监控端点
        '/manage': {
          target: baseUrl,
          changeOrigin: true,
          // 转发真实客户端 IP，后端限流/冷却按访客生效（容器网络下直连方是内网代理）
          xfwd: true
        }
        // 解决 SPA 应用 history 模式下刷新404问题
        // 在Vite中，默认支持SPA history模式，无需额外配置
      }
    },
    optimizeDeps: devOptimizeDeps,
    // CSS 配置
    css: {
      preprocessorOptions: {
        scss: {
          api: 'modern-compiler'
        }
      },
      postcss: {
        from: undefined,
        plugins: [
          {
            postcssPlugin: 'internal:charset-removal',
            AtRule: {
              charset: atRule => {
                if (atRule.name === 'charset') {
                  atRule.remove()
                }
              }
            }
          }
        ]
      }
    }
  }
})
