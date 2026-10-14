import { onBeforeUnmount, onMounted, type Ref } from 'vue'

/**
 * 滚动入场动效（企业官网模板体系）。
 *
 * 行为约定移植自 docs/design/templates/rv.js 与方案文档 §4.3/§4.7：
 * - IntersectionObserver threshold 0.15 + rootMargin 底部 -10%，触发后立即 unobserve（只播一次）；
 * - IO 不可用、或用户偏好 prefers-reduced-motion: reduce 时，元素直接加 .in 呈现终态——
 *   绝不允许因动效失败导致内容不可见（§4.7 唯一不可妥协红线）；
 * - 样式侧（.rv / .in / .d1-.d4 延迟）见 assets/styles/themes/site-templates.scss。
 */
export function useScrollReveal(root: Ref<HTMLElement | null>) {
  let io: IntersectionObserver | null = null

  const prefersReducedMotion = () =>
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches

  const revealAll = () => {
    root.value?.querySelectorAll<HTMLElement>('.rv').forEach(el => el.classList.add('in'))
  }

  onMounted(() => {
    const els = Array.from(root.value?.querySelectorAll<HTMLElement>('.rv') ?? [])
    if (els.length === 0) return

    if (
      typeof IntersectionObserver === 'undefined' ||
      prefersReducedMotion()
    ) {
      revealAll()
      return
    }

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
    els.forEach(el => io?.observe(el))
  })

  onBeforeUnmount(() => {
    io?.disconnect()
    io = null
  })
}
