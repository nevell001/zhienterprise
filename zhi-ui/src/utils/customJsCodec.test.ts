import { describe, expect, it } from 'vitest'
import { decodeCustomJs, encodeCustomJs } from './customJsCodec'

describe('custom_js codec', () => {
  it('编码后不含 script 标签，并可还原 Unicode 代码', () => {
    const code = '<script>window.__zhi = "统计 ✓"</script>'
    const encoded = encodeCustomJs(code)

    expect(encoded.startsWith('b64:')).toBe(true)
    expect(encoded).not.toContain('<script>')
    expect(decodeCustomJs(encoded)).toBe(code)
  })

  it('兼容历史明文值与空值', () => {
    expect(decodeCustomJs('<script>window.__old = 1</script>')).toBe(
      '<script>window.__old = 1</script>'
    )
    expect(decodeCustomJs('')).toBe('')
    expect(decodeCustomJs(null)).toBe('')
    expect(encodeCustomJs('   ')).toBe('')
  })
})
