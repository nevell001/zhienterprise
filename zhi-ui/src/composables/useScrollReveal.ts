import { onBeforeUnmount, onMounted, watch, type Ref } from 'vue'
import { motionFadeOnly, shouldSkipMotion } from '@/utils/motionPreference'

/**
 * 滚动入场动效（企业官网模板体系）。
 *
 * 行为约定移植自 docs/design/templates/rv.js 与方案文档 §4.3/§4.7：
 * - IntersectionObserver threshold 0.15 + rootMargin 底部 -10%，触发后立即 unobserve（只播一次）；
 * - IO 不可用、用户偏好 prefers-reduced-motion 或手动「减弱动效」时，元素直接呈现终态——
 *   绝不允许因动效失败导致内容不可见（§4.7 唯一不可妥协红线）；
 * - 性能降级（§4.7）：saveData/低内存/少核设备纯淡入（根挂 .rv-fade，无位移）；
 * - root 可能晚于挂载出现（builder 页数据返回后才渲染容器），也可能在路由复用组件时
 *   随加载态卸载又重建（如 /page/:slug 切换、详情骨架）——因此对 root 持续响应：
 *   卸载即断开观察器并重置状态，重建后重新启动；
 * - 页面挂载时只扫一遍不够：列表/详情卡片由接口异步渲染，MutationObserver 持续接管
 *   后插入的 .rv 节点（含 reduced-motion 下直接补 .in），否则这些卡片永远停在 opacity:0；
 * - 样式侧（.rv / .in / .d1-.d4 延迟）见 assets/styles/themes/site-templates.scss。
 */
export function useScrollReveal(root: Ref<HTMLElement | null>) {
  let io: IntersectionObserver | null = null
  let mo: MutationObserver | null = null
  let started = false
  let instant = false
  let fadeOnly = false
  let stopRootWatch: (() => void) | null = null

  const prefersReducedMotion = () =>
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches

  const revealAll = () => {
    root.value?.querySelectorAll<HTMLElement>('.rv').forEach(el => el.classList.add('in'))
  }

  const observeNode = (el: HTMLElement) => {
    if (el.classList.contains('in')) return
    if (instant) {
      el.classList.add('in')
      return
    }
    io?.observe(el)
  }

  const collectNewNodes = (node: Node) => {
    if (!(node instanceof HTMLElement)) return
    if (node.classList.contains('rv')) observeNode(node)
    node.querySelectorAll<HTMLElement>('.rv').forEach(observeNode)
  }

  const start = () => {
    if (started || !root.value) return
    started = true

    instant = shouldSkipMotion()
    fadeOnly = !instant && motionFadeOnly()
    if (fadeOnly) root.value.classList.add('rv-fade')

    if (typeof IntersectionObserver === 'undefined' || instant) {
      // IO 不可用：置 instant 让后续异步插入的节点也直接终态
      instant = true
      revealAll()
    } else {
      io = new IntersectionObserver(
        entries => {
          entries.forEach(entry => {
            if (entry.isIntersecting) {
              entry.target.classList.add('in')
              io?.unobserve(entry.target)
            }
          })
        },
        { threshold: 0.15, rootMargin: '0px 0px -10%' }
      )
      root.value.querySelectorAll<HTMLElement>('.rv').forEach(observeNode)
    }

    if (typeof MutationObserver === 'undefined') return
    mo = new MutationObserver(mutations => {
      mutations.forEach(mutation => {
        mutation.addedNodes.forEach(collectNewNodes)
      })
    })
    mo.observe(root.value, { childList: true, subtree: true })
  }

  const stop = () => {
    started = false
    io?.disconnect()
    io = null
    mo?.disconnect()
    mo = null
  }

  const onRootChange = (el: HTMLElement | null) => {
    // 容器随加载态卸载又重建（v-if 切换 / 路由复用组件）：重置后重新启动，
    // 否则重建容器的 .rv 永远停留在 opacity:0（整页白屏）
    stop()
    if (el) start()
  }

  onMounted(() => {
    onRootChange(root.value)
    // 持续监听容器替换（异步加载骨架 → 内容容器、路由复用组件重建）
    stopRootWatch = watch(root, el => onRootChange(el), { flush: 'post' })
  })

  onBeforeUnmount(() => {
    stopRootWatch?.()
    stopRootWatch = null
    stop()
  })
}
