import { afterEach, describe, expect, it } from 'vitest'
import { removeJsonLd, setJsonLd } from './jsonld'

afterEach(() => removeJsonLd('test-jsonld'))

describe('jsonld', () => {
  it('注入后生成 application/ld+json 的 script 节点，内容为 JSON', () => {
    setJsonLd('test-jsonld', { a: 1 })
    const el = document.getElementById('test-jsonld')
    expect(el?.tagName).toBe('SCRIPT')
    expect(el?.getAttribute('type')).toBe('application/ld+json')
    expect(JSON.parse(el?.textContent || '{}')).toEqual({ a: 1 })
  })

  it('重复注入是覆盖而非追加（upsert 语义）', () => {
    setJsonLd('test-jsonld', { a: 1 })
    setJsonLd('test-jsonld', { b: 2 })
    expect(document.querySelectorAll('#test-jsonld').length).toBe(1)
    expect(JSON.parse(document.getElementById('test-jsonld')?.textContent || '{}')).toEqual({ b: 2 })
  })

  it('移除后节点不存在；对不存在的 id 移除是安全空操作', () => {
    setJsonLd('test-jsonld', { a: 1 })
    removeJsonLd('test-jsonld')
    expect(document.getElementById('test-jsonld')).toBeNull()
    expect(() => removeJsonLd('test-jsonld')).not.toThrow()
  })
})
