<template>
  <section class="pad">
    <div class="eh-wrap">
      <div class="tpl-sec-head rv">
        <div class="sec-tag">联系方式</div>
        <h2>{{ cfg.title || '随时找到我们' }}</h2>
      </div>
      <div v-if="basicItems.length" class="ct-grid rv d1">
        <div v-for="item in basicItems" :key="item.key" class="ct-card">
          <div class="ct-type">{{ item.label }}</div>
          <a v-if="item.href" :href="item.href" class="ct-value ct-link">{{ item.value }}</a>
          <div v-else class="ct-value">{{ item.value }}</div>
        </div>
      </div>
      <p v-else class="ct-empty rv">暂未公开联系方式，可在后台「站点设置」中填写</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'

const props = defineProps<{ config?: Record<string, any> }>()

const siteSettingsStore = useSiteSettingsStore()
const blogSettings = computed(() => siteSettingsStore.siteSettings)
const cfg = computed(() => props.config ?? {})

const basicItems = computed(() => {
  const items: { key: string; label: string; value: string; href?: string }[] = []
  const phone = str(cfg.value.contact_phone) || str(blogSettings.value.contact_phone)
  const email = str(cfg.value.contact_email)
  const address = str(cfg.value.contact_address) || str(blogSettings.value.contact_address)
  if (phone) {
    items.push({
      key: 'phone',
      label: '联系电话',
      value: phone,
      href: `tel:${phone.replace(/\s/g, '')}`
    })
  }
  if (email) {
    items.push({ key: 'email', label: '联系邮箱', value: email, href: `mailto:${email}` })
  }
  if (address) {
    items.push({ key: 'address', label: '办公地址', value: address })
  }
  return items
})

const str = (v: unknown) => (typeof v === 'string' ? v.trim() : '')
</script>
