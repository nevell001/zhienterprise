import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  parseTime,
  resetForm,
  addDateRange,
  selectDictLabel,
  selectDictLabels,
  sprintf,
  parseStrEmpty,
  mergeRecursive,
  handleTree,
  tansParams,
  getNormalPath,
  blobValidate
} from './zhi'

describe('zhi 工具测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('parseTime', () => {
    it('应该导出 parseTime 函数', () => {
      expect(parseTime).toBeDefined()
      expect(typeof parseTime).toBe('function')
    })

    it('应该处理空输入', () => {
      expect(parseTime()).toBe(null)
      expect(parseTime('')).toBe(null)
    })

    it('应该格式化日期字符串', () => {
      const result = parseTime('2024-01-01', '{y}-{m}-{d}')
      expect(result).toContain('2024')
    })

    it('应该格式化Date对象', () => {
      const result = parseTime(new Date('2024-01-01'))
      expect(result).toContain('2024')
    })

    it('应该格式化时间戳', () => {
      const result = parseTime(1704067200)
      expect(result).toContain('2024')
    })

    it('应该显示星期几', () => {
      const result = parseTime(new Date('2024-01-01T00:00:00'), '{y}-{m}-{d} {a}')
      expect(result).toContain('一')
    })

    it('应该补零', () => {
      const result = parseTime('2024-01-01 9:5:6', '{h}:{i}:{s}')
      expect(result).toBe('09:05:06')
    })
  })

  describe('resetForm', () => {
    it('应该导出 resetForm 函数', () => {
      expect(resetForm).toBeDefined()
      expect(typeof resetForm).toBe('function')
    })

    it('应该重置表单字段', () => {
      const mockComponent = {
        $refs: {
          testForm: {
            resetFields: vi.fn()
          }
        }
      }
      const testContext = { $refs: mockComponent.$refs } as any
      resetForm.call(testContext, 'testForm')
      expect(mockComponent.$refs.testForm.resetFields).toHaveBeenCalled()
    })
  })

  describe('addDateRange', () => {
    it('应该导出 addDateRange 函数', () => {
      expect(addDateRange).toBeDefined()
      expect(typeof addDateRange).toBe('function')
    })

    it('应该添加日期范围参数', () => {
      const params = { params: {} }
      const dateRange = ['2024-01-01', '2024-12-31']
      const result = addDateRange(params, dateRange)
      expect(result.params).toHaveProperty('beginTime')
      expect(result.params).toHaveProperty('endTime')
    })

    it('应该处理自定义属性名', () => {
      const params = { params: {} }
      const dateRange = ['2024-01-01', '2024-12-31']
      const result = addDateRange(params, dateRange, 'test')
      expect(result.params).toHaveProperty('begintest')
      expect(result.params).toHaveProperty('endtest')
    })
  })

  describe('selectDictLabel', () => {
    it('应该导出 selectDictLabel 函数', () => {
      expect(selectDictLabel).toBeDefined()
      expect(typeof selectDictLabel).toBe('function')
    })

    it('应该返回空字符串当值为 undefined 时', () => {
      const datas = [
        { value: '1', label: '选项1' },
        { value: '2', label: '选项2' }
      ]
      expect(selectDictLabel(datas, undefined as any)).toBe('')
    })

    it('应该返回对应的标签', () => {
      const datas = [
        { value: '1', label: '选项1' },
        { value: '2', label: '选项2' }
      ]
      expect(selectDictLabel(datas, '1')).toBe('选项1')
    })

    it('当不匹配时返回原始值', () => {
      const datas = [
        { value: '1', label: '选项1' },
        { value: '2', label: '选项2' }
      ]
      expect(selectDictLabel(datas, '3')).toBe('3')
    })
  })

  describe('selectDictLabels', () => {
    it('应该导出 selectDictLabels 函数', () => {
      expect(selectDictLabels).toBeDefined()
      expect(typeof selectDictLabels).toBe('function')
    })

    it('应该返回空字符串当值为空时', () => {
      const datas = [{ value: '1', label: '选项1' }]
      expect(selectDictLabels(datas, '')).toBe('')
      expect(selectDictLabels(datas, undefined as any)).toBe('')
    })

    it('应该返回多个标签', () => {
      const datas = [
        { value: '1', label: '选项1' },
        { value: '2', label: '选项2' }
      ]
      expect(selectDictLabels(datas, '1,2')).toContain('选项1')
      expect(selectDictLabels(datas, '1,2')).toContain('选项2')
    })

    it('应该支持自定义分隔符', () => {
      const datas = [
        { value: '1', label: '选项1' },
        { value: '2', label: '选项2' }
      ]
      const result = selectDictLabels(datas, ['1', '2'], '|')
      expect(result).toContain('选项1')
      expect(result).toContain('选项2')
      expect(result).toContain('|')
    })
  })

  describe('sprintf', () => {
    it('应该导出 sprintf 函数', () => {
      expect(sprintf).toBeDefined()
      expect(typeof sprintf).toBe('function')
    })

    it('应该替换占位符', () => {
      const result = sprintf('Hello %s, your score is %s', 'Alice', '95')
      expect(result).toBe('Hello Alice, your score is 95')
    })

    it('应该处理 undefined 参数', () => {
      const result = sprintf('Hello %s, %s', 'Alice', undefined as any)
      expect(result).toBe('Hello Alice, ')
    })
  })

  describe('parseStrEmpty', () => {
    it('应该导出 parseStrEmpty 函数', () => {
      expect(parseStrEmpty).toBeDefined()
      expect(typeof parseStrEmpty).toBe('function')
    })

    it('应该返回空字符串当值为 undefined 时', () => {
      expect(parseStrEmpty(undefined as any)).toBe('')
      expect(parseStrEmpty('undefined')).toBe('')
      expect(parseStrEmpty('null')).toBe('')
    })

    it('应该返回空字符串当值为 null 时', () => {
      expect(parseStrEmpty(null as any)).toBe('')
    })

    it('应该转换其他值为字符串', () => {
      expect(parseStrEmpty(123)).toBe('123')
      expect(parseStrEmpty('test')).toBe('test')
    })
  })

  describe('mergeRecursive', () => {
    it('应该导出 mergeRecursive 函数', () => {
      expect(mergeRecursive).toBeDefined()
      expect(typeof mergeRecursive).toBe('function')
    })

    it('应该递归合并对象', () => {
      const source = { a: 1, b: { c: 2 } }
      const target = { b: { d: 3 }, e: 4 }
      const result = mergeRecursive(source, target)
      expect(result).toHaveProperty('a')
      expect(result).toHaveProperty('b')
      expect(result.b).toHaveProperty('c')
      expect(result.b).toHaveProperty('d')
      expect(result).toHaveProperty('e')
    })
  })

  describe('handleTree', () => {
    it('应该导出 handleTree 函数', () => {
      expect(handleTree).toBeDefined()
      expect(typeof handleTree).toBe('function')
    })

    it('应该构造树型结构', () => {
      const data = [
        { id: 1, parentId: 0, name: 'Root' },
        { id: 2, parentId: 1, name: 'Child1' },
        { id: 3, parentId: 1, name: 'Child2' }
      ]
      const tree = handleTree(data)
      expect(tree).toHaveLength(1)
      expect(tree[0].children).toHaveLength(2)
    })

    it('应该支持自定义字段名', () => {
      const data = [
        { key: 1, parent: 0, name: 'Root' },
        { key: 2, parent: 1, name: 'Child1' }
      ]
      const tree = handleTree(data, 'key', 'parent')
      expect(tree).toHaveLength(1)
    })
  })

  describe('tansParams', () => {
    it('应该导出 tansParams 函数', () => {
      expect(tansParams).toBeDefined()
      expect(typeof tansParams).toBe('function')
    })

    it('应该转换参数', () => {
      const params = { name: 'test', age: 18 }
      const result = tansParams(params)
      expect(result).toContain('name=')
      expect(result).toContain('age=')
      expect(result).toContain('test')
    })

    it('应该处理嵌套对象', () => {
      const params = { user: { name: 'test', age: 18 } }
      const result = tansParams(params)
      expect(result).toContain('user%5Bname%5D=')
      expect(result).toContain('user%5Bage%5D=')
    })
  })

  describe('getNormalPath', () => {
    it('应该导出 getNormalPath 函数', () => {
      expect(getNormalPath).toBeDefined()
      expect(typeof getNormalPath).toBe('function')
    })

    it('应该转换双斜杠为单斜杠', () => {
      expect(getNormalPath('//path//to//file')).toBe('/path/to/file')
    })

    it('应该移除末尾斜杠', () => {
      expect(getNormalPath('/path/to/file/')).toBe('/path/to/file')
    })

    it('应该处理空路径', () => {
      expect(getNormalPath('')).toBe('')
      expect(getNormalPath(undefined as any)).toBeUndefined()
    })
  })

  describe('blobValidate', () => {
    it('应该导出 blobValidate 函数', () => {
      expect(blobValidate).toBeDefined()
      expect(typeof blobValidate).toBe('function')
    })

    it('应该验证是否为 blob 格式', () => {
      const jsonBlob = new Blob(['{"test": "data"}'], { type: 'application/json' })
      const imageBlob = new Blob(['test'], { type: 'image/jpeg' })

      expect(blobValidate(jsonBlob)).toBe(false)
      expect(blobValidate(imageBlob)).toBe(true)
    })
  })
})
