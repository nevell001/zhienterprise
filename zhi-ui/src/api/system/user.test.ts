import { describe, it, expect, beforeEach, vi } from 'vitest'
import request from '@/utils/request'
import { parseStrEmpty } from '@/utils/zhi'
import * as userApi from './user'

// Mock dependencies
vi.mock('@/utils/request', () => ({
  default: vi.fn(() => Promise.resolve({ data: {} }))
}))

vi.mock('@/utils/zhi', () => ({
  parseStrEmpty: vi.fn(val => val || '')
}))

describe('System User API 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('listUser', () => {
    it('应该导出 listUser 函数', () => {
      expect(userApi.listUser).toBeDefined()
      expect(typeof userApi.listUser).toBe('function')
    })

    it('应该调用用户列表接口', () => {
      userApi.listUser({ pageNum: 1, pageSize: 10 })

      expect(request).toHaveBeenCalledWith({
        url: '/system/user/list',
        method: 'get',
        params: { pageNum: 1, pageSize: 10 }
      })
    })
  })

  describe('getUser', () => {
    it('应该导出 getUser 函数', () => {
      expect(userApi.getUser).toBeDefined()
      expect(typeof userApi.getUser).toBe('function')
    })

    it('应该调用用户详情接口', () => {
      userApi.getUser(123)

      expect(parseStrEmpty).toHaveBeenCalledWith(123)
      expect(request).toHaveBeenCalledWith({
        url: '/system/user/123',
        method: 'get'
      })
    })
  })

  describe('addUser', () => {
    it('应该导出 addUser 函数', () => {
      expect(userApi.addUser).toBeDefined()
      expect(typeof userApi.addUser).toBe('function')
    })

    it('应该调用新增用户接口', () => {
      const userData = { userName: 'test', nickName: 'Test User' }
      userApi.addUser(userData)

      expect(request).toHaveBeenCalledWith({
        url: '/system/user',
        method: 'post',
        data: userData
      })
    })
  })

  describe('updateUser', () => {
    it('应该导出 updateUser 函数', () => {
      expect(userApi.updateUser).toBeDefined()
      expect(typeof userApi.updateUser).toBe('function')
    })

    it('应该调用修改用户接口', () => {
      const userData = { userId: 123, userName: 'test' }
      userApi.updateUser(userData)

      expect(request).toHaveBeenCalledWith({
        url: '/system/user',
        method: 'put',
        data: userData
      })
    })
  })

  describe('delUser', () => {
    it('应该导出 delUser 函数', () => {
      expect(userApi.delUser).toBeDefined()
      expect(typeof userApi.delUser).toBe('function')
    })

    it('应该调用删除用户接口', () => {
      userApi.delUser(123)

      expect(request).toHaveBeenCalledWith({
        url: '/system/user/123',
        method: 'delete'
      })
    })
  })

  describe('resetUserPwd', () => {
    it('应该导出 resetUserPwd 函数', () => {
      expect(userApi.resetUserPwd).toBeDefined()
      expect(typeof userApi.resetUserPwd).toBe('function')
    })

    it('应该调用重置用户密码接口', () => {
      userApi.resetUserPwd(123, 'newPassword123')

      expect(request).toHaveBeenCalledWith({
        url: '/system/user/resetPwd',
        method: 'put',
        data: { userId: 123, password: 'newPassword123' }
      })
    })
  })

  describe('changeUserStatus', () => {
    it('应该导出 changeUserStatus 函数', () => {
      expect(userApi.changeUserStatus).toBeDefined()
      expect(typeof userApi.changeUserStatus).toBe('function')
    })

    it('应该调用修改用户状态接口', () => {
      userApi.changeUserStatus(123, '1')

      expect(request).toHaveBeenCalledWith({
        url: '/system/user/changeStatus',
        method: 'put',
        data: { userId: 123, status: '1' }
      })
    })
  })

  describe('uploadAvatar', () => {
    it('应该导出 uploadAvatar 函数', () => {
      expect(userApi.uploadAvatar).toBeDefined()
      expect(typeof userApi.uploadAvatar).toBe('function')
    })

    it('应该调用上传用户头像接口', () => {
      const formData = new FormData()
      formData.append('file', new File([''], 'avatar.jpg'))
      userApi.uploadAvatar(formData)

      expect(request).toHaveBeenCalledWith({
        url: '/system/user/profile/avatar',
        method: 'post',
        data: formData
      })
    })
  })

  describe('getAuthRole', () => {
    it('应该导出 getAuthRole 函数', () => {
      expect(userApi.getAuthRole).toBeDefined()
      expect(typeof userApi.getAuthRole).toBe('function')
    })

    it('应该调用查询授权角色接口', () => {
      userApi.getAuthRole(123)

      expect(request).toHaveBeenCalledWith({
        url: '/system/user/authRole/123',
        method: 'get'
      })
    })
  })

  describe('updateAuthRole', () => {
    it('应该导出 updateAuthRole 函数', () => {
      expect(userApi.updateAuthRole).toBeDefined()
      expect(typeof userApi.updateAuthRole).toBe('function')
    })

    it('应该调用保存授权角色接口', () => {
      const roleData = { userId: 123, roleIds: [1, 2] }
      userApi.updateAuthRole(roleData)

      expect(request).toHaveBeenCalledWith({
        url: '/system/user/authRole',
        method: 'put',
        params: roleData
      })
    })
  })

  describe('deptTreeSelect', () => {
    it('应该导出 deptTreeSelect 函数', () => {
      expect(userApi.deptTreeSelect).toBeDefined()
      expect(typeof userApi.deptTreeSelect).toBe('function')
    })

    it('应该调用查询部门下拉树结构接口', () => {
      userApi.deptTreeSelect()

      expect(request).toHaveBeenCalledWith({
        url: '/system/user/deptTree',
        method: 'get'
      })
    })
  })
})
