import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import BannerCarousel from './BannerCarousel.vue'

/**
 * 首页 Banner 轮播行为测试。
 *
 * 约定（方案文档 §4.3/§4.7）：
 * - 单图：只渲染一张，不出现圆点/箭头等轮播控件；
 * - 多图：自动轮播（5s）、箭头/圆点手动切换、悬停暂停；
 * - 「减弱动效」（html.motion-off 或系统偏好）时**不自动播放**，但手动切换仍可用。
 */
const images = ['/profile/a.png', '/profile/b.png', '/profile/c.png']

const activeIndex = (wrapper: ReturnType<typeof mount>) =>
  wrapper.findAll('.bc-slide').findIndex(node => node.classes().includes('is-active'))

describe('BannerCarousel 首页 Banner 轮播', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    document.documentElement.classList.remove('motion-off')
  })

  afterEach(() => {
    vi.useRealTimers()
    document.documentElement.classList.remove('motion-off')
  })

  it('单图只渲染一张且没有轮播控件', () => {
    const wrapper = mount(BannerCarousel, { props: { images: ['/profile/only.png'] } })

    expect(wrapper.findAll('.bc-slide')).toHaveLength(1)
    expect(wrapper.find('.bc-dots').exists()).toBe(false)
    expect(wrapper.find('.bc-nav').exists()).toBe(false)
    expect(wrapper.find('.banner-carousel').classes()).toContain('is-single')
  })

  it('多图渲染全部图片并显示圆点指示器，默认第一张', () => {
    const wrapper = mount(BannerCarousel, { props: { images } })

    expect(wrapper.findAll('.bc-slide')).toHaveLength(images.length)
    expect(wrapper.findAll('.bc-dot')).toHaveLength(images.length)
    expect(activeIndex(wrapper)).toBe(0)
  })

  it('箭头与圆点可手动切换（首尾循环）', async () => {
    const wrapper = mount(BannerCarousel, { props: { images } })

    await wrapper.find('.bc-next').trigger('click')
    expect(activeIndex(wrapper)).toBe(1)

    await wrapper.find('.bc-prev').trigger('click')
    await wrapper.find('.bc-prev').trigger('click')
    expect(activeIndex(wrapper)).toBe(images.length - 1)

    await wrapper.findAll('.bc-dot')[2].trigger('click')
    expect(activeIndex(wrapper)).toBe(2)
  })

  it('多图自动轮播：间隔 5s 切到下一张', async () => {
    const wrapper = mount(BannerCarousel, { props: { images } })
    expect(activeIndex(wrapper)).toBe(0)

    vi.advanceTimersByTime(5000)
    await wrapper.vm.$nextTick()
    expect(activeIndex(wrapper)).toBe(1)

    vi.advanceTimersByTime(5000)
    await wrapper.vm.$nextTick()
    expect(activeIndex(wrapper)).toBe(2)

    // 循环回第一张
    vi.advanceTimersByTime(5000)
    await wrapper.vm.$nextTick()
    expect(activeIndex(wrapper)).toBe(0)
  })

  it('悬停暂停自动轮播，移出后恢复', async () => {
    const wrapper = mount(BannerCarousel, { props: { images } })
    const root = wrapper.find('.banner-carousel')

    await root.trigger('mouseenter')
    vi.advanceTimersByTime(10000)
    await wrapper.vm.$nextTick()
    expect(activeIndex(wrapper)).toBe(0)

    await root.trigger('mouseleave')
    vi.advanceTimersByTime(5000)
    await wrapper.vm.$nextTick()
    expect(activeIndex(wrapper)).toBe(1)
  })

  it('减弱动效（html.motion-off）时不自动播放，但手动切换仍可用', async () => {
    document.documentElement.classList.add('motion-off')
    const wrapper = mount(BannerCarousel, { props: { images } })

    vi.advanceTimersByTime(15000)
    await wrapper.vm.$nextTick()
    expect(activeIndex(wrapper)).toBe(0)

    await wrapper.find('.bc-next').trigger('click')
    expect(activeIndex(wrapper)).toBe(1)
  })

  it('触摸滑动可切换（左滑下一张、右滑上一张）', async () => {
    const wrapper = mount(BannerCarousel, { props: { images } })
    const track = wrapper.find('.bc-track')

    track.element.dispatchEvent(
      new TouchEvent('touchstart', { touches: [{ clientX: 300 }] } as never)
    )
    track.element.dispatchEvent(
      new TouchEvent('touchend', { changedTouches: [{ clientX: 120 }] } as never)
    )
    await wrapper.vm.$nextTick()
    expect(activeIndex(wrapper)).toBe(1)

    track.element.dispatchEvent(
      new TouchEvent('touchstart', { touches: [{ clientX: 120 }] } as never)
    )
    track.element.dispatchEvent(
      new TouchEvent('touchend', { changedTouches: [{ clientX: 300 }] } as never)
    )
    await wrapper.vm.$nextTick()
    expect(activeIndex(wrapper)).toBe(0)
  })
})
