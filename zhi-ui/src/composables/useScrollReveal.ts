import { onBeforeUnmount, onMounted, watch, type Ref } from 'vue'

/**
 * 滚动入场动效（企业官网模板体系）。
 *
 * 行为约定移植自 docs/design/templates/rv.js 与方案文档 §4.3/§4.7：
 * - IntersectionObserver threshold 0.15 + rootMargin 底部 -10%，触发后立即 unobserve（只播一次）；
 * - IO 不可用、或用户偏好 prefers-reduced-motion: reduce 时，元素直接加 .in 呈现终态——
 *   绝不允许因动效失败导致内容不可见（§4.7 唯一不可妥协红线）；
 * - root 可能晚于挂载才出现（builder 页数据返回后才渲染容器，如 /page/:slug）：
 *   此时挂载就扫一遍会落空，必须等容器出现后再启动，否则其中的 .rv 永远停在 opacity:0；
 * - 页面挂载时只扫一遍不够：列表/详情卡片由接口异步渲染，MutationObserver 持续接管
 *   后插入的 .rv 节点（含 reduced-motion 下直接补 .in），否则这些卡片永远停在 opacity:0；
 * - 样式侧（.rv / .in / .d1-.d4 延迟）见 assets/styles/themes/site-templates.scss。
 */
export function useScrollReveal(root: Ref<HTMLElement | null>) {
  let io: IntersectionObserver | null = null
  let mo: MutationObserver | null = null
  // reduced-motion / IO 不可用时置位：新节点加入后直接呈现终态
  let instant = false
  let started = false
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

    if (typeof IntersectionObserver === 'undefined' || prefersReducedMotion()) {
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

  onMounted(() => {
    if (root.value) {
      start()
      return
    }
    // 容器晚于挂载出现（异步数据驱动渲染）：出现后再启动观察
    stopRootWatch = watch(
      root,
      el => {
        if (!el) return
        stopRootWatch?.()
        stopRootWatch = null
        start()
      },
      { flush: 'post' }
    )
  })

  onBeforeUnmount(() => {
    stopRootWatch?.()
    stopRootWatch = null
    io?.disconnect()
    io = null
    mo?.disconnect()
    mo = null
  })
}
