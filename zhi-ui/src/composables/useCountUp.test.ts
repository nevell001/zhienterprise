import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { defineComponent, nextTick, ref } from 'vue'
import { mount } from '@vue/test-utils'
import { useCountUp } from './useCountUp'

/**
 * jsdom 没有 IntersectionObserver 原生实现，这里用替身记录被观察的节点，
 * 并手动触发「进入视口」回调；rAF 同样替身为手动帧队列，用帧时间戳驱动动画，
 * 断言数字滚动过程中的真实文本（前后缀保留、终值精确落位）。
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

let rafQueue: Array<(ts: number) => void> = []
let rafSeq = 0
const cancelRaf = vi.fn()

const stubRaf = () => {
  rafQueue = []
  rafSeq = 0
  vi.stubGlobal('requestAnimationFrame', (cb: (ts: number) => void) => {
    rafQueue.push(cb)
    return ++rafSeq
  })
  vi.stubGlobal('cancelAnimationFrame', cancelRaf)
}

/** 执行当前排队的一帧（模拟 1200ms 动画期间的某一时刻） */
const flushFrame = (ts: number) => {
  const queue = rafQueue
  rafQueue = []
  queue.forEach(cb => cb(ts))
}

const stubMatchMedia = (reduceMatches: boolean) => {
  vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({ matches: reduceMatches } as MediaQueryList))
}

/**
 * 静态多个计数节点 + 条件渲染的异步节点（模拟接口返回后插入的指标/成果）。
 * - withStatic=false：模拟挂载时无计数节点；
 * - lateRoot=true：根容器晚于挂载出现（builder 页接口返回后才渲染容器）。
 */
const Harness = defineComponent({
  props: {
    withStatic: { type: Boolean, default: true },
    lateRoot: { type: Boolean, default: false }
  },
  setup(props) {
    const rootEl = ref<HTMLElement | null>(null)
    const showRoot = ref(!props.lateRoot)
    const showAsync = ref(false)
    useCountUp(rootEl)
    return { rootEl, showRoot, showAsync }
  },
  template: `
    <div v-if="showRoot" ref="rootEl">
      <template v-if="withStatic">
        <b data-test="plain" data-count-up="1200+">1200+</b>
        <b data-test="grouped" data-count-up="1,200">1,200</b>
        <b data-test="text" data-count-up="覆盖全国">覆盖全国</b>
      </template>
      <b v-if="showAsync" data-test="async" data-count-up="↓38%">↓38%</b>
    </div>
  `
})

const flush = () => new Promise(resolve => setTimeout(resolve, 0))

describe('useCountUp（数字滚动计数）', () => {
  beforeEach(() => {
    FakeIntersectionObserver.instances = []
    vi.stubGlobal('IntersectionObserver', FakeIntersectionObserver)
    stubRaf()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    cancelRaf.mockClear()
  })

  it('进入视口后从 0 滚动到目标值（1200ms easeOutExpo），只播一次', () => {
    const wrapper = mount(Harness)
    const io = FakeIntersectionObserver.instances[0]
    const el = wrapper.get('[data-test="plain"]').element

    expect(io.observed.has(el)).toBe(true)
    // 未进入视口不启动，也不改写文本
    expect(el.textContent).toBe('1200+')

    io.trigger()
    expect(el.textContent).toBe('0+')

    flushFrame(1000) // 起始帧
    expect(el.textContent).toBe('0+')

    flushFrame(1600) // 600ms：easeOutExpo(0.5)=0.96875 → 1162.5 → 取整 1163
    expect(el.textContent).toBe('1163+')

    flushFrame(2200) // ≥1200ms：精确落位终值
    expect(el.textContent).toBe('1200+')
    expect(io.observed.has(el)).toBe(false)
  })

  it('千分位与小数格式在动画全程保留', () => {
    const wrapper = mount(Harness)
    const io = FakeIntersectionObserver.instances[0]
    const grouped = wrapper.get('[data-test="grouped"]').element

    io.trigger()
    expect(grouped.textContent).toBe('0')

    flushFrame(1000)
    flushFrame(2200)

    expect(grouped.textContent).toBe('1,200')
  })

  it('不含数字的文本保持原样且不启动动画', () => {
    const wrapper = mount(Harness)
    const io = FakeIntersectionObserver.instances[0]
    const textEl = wrapper.get('[data-test="text"]').element

    io.trigger()
    flushFrame(1000)
    flushFrame(2200)

    expect(textEl.textContent).toBe('覆盖全国')
  })

  it('异步插入的计数节点被接管，前后缀（↓ 与 %）在动画全程保留', async () => {
    const wrapper = mount(Harness, { props: { withStatic: false } })
    const io = FakeIntersectionObserver.instances[0]

    wrapper.vm.showAsync = true
    await nextTick()
    await flush()

    const asyncEl = wrapper.get('[data-test="async"]').element
    expect(io.observed.has(asyncEl)).toBe(true)

    io.trigger()
    expect(asyncEl.textContent).toBe('↓0%')

    flushFrame(1000)
    flushFrame(2200)
    expect(asyncEl.textContent).toBe('↓38%')
  })

  it('root 晚于挂载出现时，容器出现后其中的计数节点被接管', async () => {
    const wrapper = mount(Harness, { props: { lateRoot: true, withStatic: false } })
    expect(FakeIntersectionObserver.instances.length).toBe(0)

    wrapper.vm.showRoot = true
    await nextTick()
    await flush()

    const io = FakeIntersectionObserver.instances[0]
    expect(io).toBeDefined()

    wrapper.vm.showAsync = true
    await nextTick()
    await flush()

    const asyncEl = wrapper.get('[data-test="async"]').element
    expect(io.observed.has(asyncEl)).toBe(true)

    io.trigger()
    flushFrame(1000)
    flushFrame(2200)
    expect(asyncEl.textContent).toBe('↓38%')
  })

  it('reduced-motion 下直接显示最终数值，不进入滚动', async () => {
    stubMatchMedia(true)

    const wrapper = mount(Harness)
    const el = wrapper.get('[data-test="plain"]').element

    expect(el.textContent).toBe('1200+')
    expect(rafQueue.length).toBe(0)

    // 异步节点同样直接呈现终值
    wrapper.vm.showAsync = true
    await nextTick()
    await flush()

    expect(wrapper.get('[data-test="async"]').element.textContent).toBe('↓38%')
    expect(rafQueue.length).toBe(0)
  })

  it('IntersectionObserver 不可用时直接显示最终数值', () => {
    vi.stubGlobal('IntersectionObserver', undefined)

    const wrapper = mount(Harness)

    expect(wrapper.get('[data-test="plain"]').element.textContent).toBe('1200+')
    expect(rafQueue.length).toBe(0)
  })

  it('卸载时取消未完成的帧并断开 observer', () => {
    const moDisconnect = vi.spyOn(MutationObserver.prototype, 'disconnect')

    const wrapper = mount(Harness)
    const io = FakeIntersectionObserver.instances[0]

    io.trigger()
    // 两个数字节点各排队一帧；「覆盖全国」不含数字不排队
    expect(rafQueue.length).toBe(2)

    wrapper.unmount()

    expect(cancelRaf).toHaveBeenCalled()
    expect(io.disconnect).toHaveBeenCalled()
    expect(moDisconnect).toHaveBeenCalled()

    moDisconnect.mockRestore()
  })
})
