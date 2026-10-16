import { onBeforeUnmount, onMounted, watch, type Ref } from 'vue'
import { shouldSkipMotion } from '@/utils/motionPreference'

/**
 * 数字滚动计数（企业官网模板体系，方案文档 §3.1/§4.7）。
 *
 * - 容器内 [data-count-up] 元素进入视口后数字从 0 滚动到目标值：
 *   1200ms、easeOutExpo（方案文档 §3.1），触发一次后不再重播；
 * - 目标值取自 data-count-up（缺省回退元素文本），动画只替换其中的数字部分，
 *   `1200+`、`↓38%`、`1,200` 等前后缀/千分位/小数原样保留；不含数字的文本不处理；
 * - prefers-reduced-motion: reduce 或 IntersectionObserver 不可用时直接呈现最终数值
 *   （§4.7 硬性要求），绝不出现因动效失败而看不到数字的情况；
 * - 与 useScrollReveal 相同的容错约定：容器晚于挂载出现（builder 页接口返回后才渲染）
 *   或节点由接口数据异步插入时，同样会被接管。
 */
const COUNT_DURATION = 1200
const easeOutExpo = (t: number) => (t >= 1 ? 1 : 1 - Math.pow(2, -10 * t))

interface ParsedCount {
  prefix: string
  suffix: string
  target: number
  decimals: number
  grouped: boolean
}

/** 提取文本里第一个数字（支持千分位与小数）：`↓38%` → 前缀 ↓ / 38 / 后缀 % */
function parseCount(raw: string): ParsedCount | null {
  const match = raw.match(/-?\d[\d,]*(?:\.\d+)?/)
  if (!match || match.index === undefined) return null
  const numText = match[0]
  const target = Number(numText.replace(/,/g, ''))
  if (!Number.isFinite(target)) return null
  const dot = numText.indexOf('.')
  return {
    prefix: raw.slice(0, match.index),
    suffix: raw.slice(match.index + numText.length),
    target,
    decimals: dot >= 0 ? numText.length - dot - 1 : 0,
    grouped: numText.includes(',')
  }
}

function formatCount(value: number, parsed: ParsedCount) {
  const rounded = parsed.decimals > 0 ? value.toFixed(parsed.decimals) : String(Math.round(value))
  const body = parsed.grouped ? Number(rounded).toLocaleString('zh-CN') : rounded
  return `${parsed.prefix}${body}${parsed.suffix}`
}

export function useCountUp(root: Ref<HTMLElement | null>) {
  const rafIds = new Set<number>()
  let io: IntersectionObserver | null = null
  let mo: MutationObserver | null = null
  // reduced-motion / IO 不可用时置位：节点出现即呈现终值
  let instant = false
  let started = false
  let stopRootWatch: (() => void) | null = null

  /** 从 0 滚动到目标值；无数字/instant 场景只保证终值落位 */
  const animate = (el: HTMLElement) => {
    const raw = el.dataset.countUp ?? el.textContent ?? ''
    const parsed = parseCount(raw)
    if (!parsed) return
    if (instant) {
      el.textContent = raw
      return
    }
    el.textContent = formatCount(0, parsed)
    let start: number | null = null
    let id = 0
    const step = (ts: number) => {
      rafIds.delete(id)
      if (start === null) start = ts
      const progress = Math.min((ts - start) / COUNT_DURATION, 1)
      el.textContent = formatCount(parsed.target * easeOutExpo(progress), parsed)
      if (progress < 1) {
        id = requestAnimationFrame(step)
        rafIds.add(id)
      }
    }
    id = requestAnimationFrame(step)
    rafIds.add(id)
  }

  const observeNode = (el: HTMLElement) => {
    if (instant) {
      animate(el)
      return
    }
    io?.observe(el)
  }

  const collectNewNodes = (node: Node) => {
    if (!(node instanceof HTMLElement)) return
    if (node.matches('[data-count-up]')) observeNode(node)
    node.querySelectorAll<HTMLElement>('[data-count-up]').forEach(observeNode)
  }

  const start = () => {
    if (started || !root.value) return
    started = true

    if (typeof IntersectionObserver === 'undefined' || shouldSkipMotion()) {
      instant = true
      root.value.querySelectorAll<HTMLElement>('[data-count-up]').forEach(animate)
    } else {
      io = new IntersectionObserver(
        entries => {
          entries.forEach(entry => {
            if (entry.isIntersecting) {
              animate(entry.target as HTMLElement)
              io?.unobserve(entry.target)
            }
          })
        },
        { threshold: 0.15, rootMargin: '0px 0px -10%' }
      )
      root.value.querySelectorAll<HTMLElement>('[data-count-up]').forEach(observeNode)
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
    rafIds.forEach(id => cancelAnimationFrame(id))
    rafIds.clear()
    io?.disconnect()
    io = null
    mo?.disconnect()
    mo = null
  })
}
