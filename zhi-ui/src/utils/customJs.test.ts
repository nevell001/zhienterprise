import { describe, expect, it } from 'vitest'
import { injectCustomJs } from './customJs'

describe('injectCustomJs', () => {
  it('保留 script 属性与内联代码，并渲染普通节点', () => {
    const container = document.createElement('div')
    const cleanup = injectCustomJs(
      '<script src="https://analytics.example.com/a.js" async></script><span class="mark">统计</span><script>window.__zhi = 1</script>',
      container
    )

    const scripts = container.querySelectorAll('script')
    expect(scripts).toHaveLength(2)
    expect(scripts[0].getAttribute('src')).toBe('https://analytics.example.com/a.js')
    expect(scripts[0].hasAttribute('async')).toBe(true)
    expect(scripts[1].text).toContain('window.__zhi = 1')
    expect(container.querySelector('.mark')?.textContent).toBe('统计')

    cleanup()
    expect(container.children).toHaveLength(0)
  })

  it('空代码不注入任何节点', () => {
    const container = document.createElement('div')
    const cleanup = injectCustomJs('   ', container)

    expect(container.children).toHaveLength(0)
    cleanup()
    expect(container.children).toHaveLength(0)
  })
})
