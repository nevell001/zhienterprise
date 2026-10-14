import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { defineComponent, nextTick, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { useScrollReveal } from './useScrollReveal'

/**
 * jsdom 没有 IntersectionObserver 原生实现，这里用替身记录被观察的节点，
 * 并由测试手动触发“进入视口”回调，直接断言 .in 的落点。
 */
class FakeIntersectionObserver {
  static instances: FakeIntersectionObserver[] = []
  callback: IntersectionObserverCallback
  observed = new Set<Element>()
  disconnect = vi.fn(() => this.observed.clear())

  constructor(callback: IntersectionObserverCallback) {
    this.callback = callback
    FakeIntersectionObserver.instances.push(this)
  }

  observe(el: Element) {
    this.observed.add(el)
  }

  unobserve(el: Element) {
    this.observed.delete(el)
  }

  takeRecords(): IntersectionObserverEntry[] {
    return []
  }

  trigger(isIntersecting = true) {
    const entries = [...this.observed].map(target => ({
      target,
      isIntersecting
    })) as unknown as IntersectionObserverEntry[]
    this.callback(entries, this as unknown as IntersectionObserver)
  }
}

/** 静态 .rv + 条件渲染的异步 .rv（模拟接口返回后出现的卡片）；withStatic=false 模拟挂载时无 .rv */
const Harness = defineComponent({
  props: {
    withStatic: { type: Boolean, default: true }
  },
  setup() {
    const rootEl = ref<HTMLElement | null>(null)
    const showAsync = ref(false)
    useScrollReveal(rootEl)
    return { rootEl, showAsync }
  },
  template: `
    <div ref="rootEl">
      <div v-if="withStatic" class="rv" data-test="static">静态卡片</div>
      <div v-if="showAsync" class="rv" data-test="async">异步卡片</div>
    </div>
  `
})

const flush = () => new Promise(resolve => setTimeout(resolve, 0))

const stubMatchMedia = (reduceMatches: boolean) => {
  vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({ matches: reduceMatches } as MediaQueryList))
}

describe('useScrollReveal（企业官网模板滚动入场）', () => {
  beforeEach(() => {
    FakeIntersectionObserver.instances = []
    vi.stubGlobal('IntersectionObserver', FakeIntersectionObserver)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('挂载时的 .rv 节点被观察，进入视口后加 .in 并停止观察', () => {
    const wrapper = mount(Harness)
    const io = FakeIntersectionObserver.instances[0]
    expect(io).toBeDefined()

    const staticEl = wrapper.get('[data-test="static"]').element
    expect(io.observed.has(staticEl)).toBe(true)

    io.trigger()

    expect(staticEl.classList.contains('in')).toBe(true)
    expect(io.observed.has(staticEl)).toBe(false)
  })

  it('异步渲染的 .rv 卡片会被接管，进入视口后可见（只扫一遍挂载会使卡片永久不可见）', async () => {
    const wrapper = mount(Harness)
    const io = FakeIntersectionObserver.instances[0]

    wrapper.vm.showAsync = true
    await nextTick()
    await flush()

    const asyncEl = wrapper.get('[data-test="async"]').element
    expect(io.observed.has(asyncEl)).toBe(true)
    expect(asyncEl.classList.contains('in')).toBe(false)

    io.trigger()

    expect(asyncEl.classList.contains('in')).toBe(true)
  })

  it('挂载时没有 .rv 也不放弃：只含异步内容的页面仍能入场', async () => {
    const wrapper = mount(Harness, { props: { withStatic: false } })
    const io = FakeIntersectionObserver.instances[0]

    wrapper.vm.showAsync = true
    await nextTick()
    await flush()

    const lateEl = wrapper.get('[data-test="async"]').element
    expect(io.observed.has(lateEl)).toBe(true)

    io.trigger()

    expect(lateEl.classList.contains('in')).toBe(true)
  })

  it('reduced-motion 下初始与异步节点都直接呈现终态', async () => {
    stubMatchMedia(true)

    const wrapper = mount(Harness)
    const staticEl = wrapper.get('[data-test="static"]').element
    expect(staticEl.classList.contains('in')).toBe(true)

    wrapper.vm.showAsync = true
    await nextTick()
    await flush()

    const asyncEl = wrapper.get('[data-test="async"]').element
    expect(asyncEl.classList.contains('in')).toBe(true)
  })

  it('IntersectionObserver 不可用时全部直接呈现终态（动效失败不允许内容不可见）', async () => {
    vi.stubGlobal('IntersectionObserver', undefined)

    const wrapper = mount(Harness)
    const staticEl = wrapper.get('[data-test="static"]').element
    expect(staticEl.classList.contains('in')).toBe(true)

    wrapper.vm.showAsync = true
    await nextTick()
    await flush()

    const asyncEl = wrapper.get('[data-test="async"]').element
    expect(asyncEl.classList.contains('in')).toBe(true)
  })

  it('卸载时断开两个 observer', () => {
    const moDisconnect = vi.spyOn(MutationObserver.prototype, 'disconnect')

    const wrapper = mount(Harness)
    const io = FakeIntersectionObserver.instances[0]

    wrapper.unmount()

    expect(io.disconnect).toHaveBeenCalled()
    expect(moDisconnect).toHaveBeenCalled()

    moDisconnect.mockRestore()
  })
})
