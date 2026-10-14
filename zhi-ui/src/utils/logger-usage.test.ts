import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const guardedUtils = ['siteSettings.ts', 'imageUtils.ts', 'permission.ts', 'safe-watch.ts']

describe('logger usage guards', () => {
  it('shared utils should use logger instead of direct console output', () => {
    guardedUtils.forEach(file => {
      const content = readFileSync(resolve(__dirname, file), 'utf8')

      expect(content).not.toMatch(/console\.(warn|error|log)/)
    })
  })
})
