<template>
  <el-form ref="userRef" :model="form" :rules="rules" label-width="80px">
    <el-form-item label="用户昵称" prop="nickName">
      <el-input v-model="form.nickName" maxlength="30" />
    </el-form-item>
    <el-form-item label="手机号码" prop="phonenumber">
      <el-input v-model="form.phonenumber" maxlength="11" />
    </el-form-item>
    <el-form-item label="邮箱" prop="email">
      <el-input v-model="form.email" maxlength="50" />
    </el-form-item>
    <el-form-item label="性别">
      <el-radio-group v-model="form.sex">
        <el-radio value="0">男</el-radio>
        <el-radio value="1">女</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="submit">保存</el-button>
      <el-button type="danger" @click="close">关闭</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { updateUserProfile } from '@/api/system/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const props = defineProps({
  user: {
    type: Object
  }
})

const { proxy } = getCurrentInstance()

interface UserForm {
  nickName?: string
  phonenumber?: string
  email?: string
  sex?: string
}

const form = ref<UserForm>({})
const rules = ref({
  nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }],
  email: [
    { required: true, message: '邮箱地址不能为空', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  phonenumber: [
    // 仅校验格式、不强制填写：注册用户只填邮箱，必填会卡死资料保存
    { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
})

/** 提交按钮 */
function submit() {
  ;(proxy.$refs.userRef as any).validate((valid: boolean) => {
    if (valid) {
      updateUserProfile({ ...props.user, ...form.value } as any).then(() => {
        ;(proxy as any).$modal.msgSuccess('修改成功')
        props.user.nickName = form.value.nickName
        props.user.phonenumber = form.value.phonenumber
        props.user.email = form.value.email
        props.user.sex = form.value.sex
        // 同步顶栏昵称（Navbar 读的是 user store，不同步要整页刷新才更新）
        userStore.nickName = form.value.nickName || userStore.nickName
      })
    }
  })
}

/** 关闭按钮 */
function close() {
  ;(proxy as any).$tab.closePage()
}

// 回显当前登录用户信息
watch(
  () => props.user,
  user => {
    if (user) {
      form.value = {
        nickName: user.nickName,
        phonenumber: user.phonenumber,
        email: user.email,
        sex: user.sex
      }
    }
  },
  { immediate: true }
)
</script>
