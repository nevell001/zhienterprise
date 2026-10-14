<template>
  <div class="contact-page mo-home-page">
    <header class="ct-head">
      <div class="ct-container">
        <h1>联系我们</h1>
        <p>期待与您的沟通与合作</p>
      </div>
    </header>

    <main class="ct-main ct-container">
      <div v-if="contactItems.length" class="ct-grid">
        <div v-for="item in contactItems" :key="item.key" class="ct-card">
          <div class="ct-icon" aria-hidden="true">
            {{ item.emoji }}
          </div>
          <div class="ct-type">
            {{ item.label }}
          </div>
          <a v-if="item.href" :href="item.href" class="ct-value ct-link">{{ item.value }}</a>
          <div v-else class="ct-value">{{ item.value }}</div>
        </div>
      </div>
      <el-empty v-else description="暂未公开联系方式，可在后台「站点设置」中填写" />

      <div v-if="isGuestbookEnabled" class="ct-cta">
        <p>有任何合作意向或问题？</p>
        <router-link class="ct-btn" to="/blog/guestbook">前往留言板</router-link>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useBlogSettingsStore } from '@/stores/blogSettings'
import { applySeo, canonicalUrl } from '@/utils/seo'

interface ContactItem {
  key: string
  label: string
  value: string
  href?: string
  emoji: string
}

const blogSettingsStore = useBlogSettingsStore()
const settings = computed(() => blogSettingsStore.blogSettings)
const isGuestbookEnabled = computed(() => blogSettingsStore.isFeatureEnabled('guestbook_enabled'))

const contactItems = computed<ContactItem[]>(() => {
  const items: ContactItem[] = []
  if (settings.value.contact_phone) {
    items.push({
      key: 'phone',
      label: '联系电话',
      value: settings.value.contact_phone,
      href: `tel:${settings.value.contact_phone.replace(/\s/g, '')}`,
      emoji: '☎️'
    })
  }
  if (settings.value.blog_email) {
    items.push({
      key: 'email',
      label: '联系邮箱',
      value: settings.value.blog_email,
      href: `mailto:${settings.value.blog_email}`,
      emoji: '✉️'
    })
  }
  if (settings.value.contact_address) {
    items.push({
      key: 'address',
      label: '办公地址',
      value: settings.value.contact_address,
      emoji: '📍'
    })
  }
  return items
})

onMounted(() => {
  const siteName = settings.value.blog_name || '知企建站系统'
  applySeo({
    title: `联系我们 - ${siteName}`,
    description: settings.value.blog_desc || '',
    keywords: '',
    canonical: canonicalUrl()
  })
})
</script>

<style scoped>
.ct-container {
  max-width: 880px;
  margin: 0 auto;
  padding: 0 var(--mo-sp-5);
}

.ct-head {
  padding: var(--mo-sp-8) 0;
  text-align: center;
  color: var(--mo-n50);
  background:
    radial-gradient(circle at 18% 20%, rgba(255, 255, 255, 0.14), transparent 42%),
    linear-gradient(135deg, var(--mo-p700), var(--mo-p900));
}

.ct-head h1 {
  margin: 0 0 var(--mo-sp-2);
  font-size: var(--mo-fs-2xl);
  letter-spacing: 1px;
}

.ct-head p {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: var(--mo-fs-sm);
}

.ct-main {
  padding: var(--mo-sp-8) var(--mo-sp-5) var(--mo-sp-9);
}

.ct-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--mo-sp-5);
}

.ct-card {
  padding: var(--mo-sp-6);
  text-align: center;
  border: 1px solid var(--mo-n200);
  border-radius: var(--mo-r-xl);
  background: var(--el-bg-color);
  box-shadow: var(--mo-shadow-sm);
}

.ct-icon {
  font-size: 30px;
  margin-bottom: var(--mo-sp-3);
}

.ct-type {
  color: var(--mo-n500);
  font-size: var(--mo-fs-sm);
  margin-bottom: var(--mo-sp-2);
}

.ct-value {
  color: var(--mo-n900);
  font-size: var(--mo-fs-md);
  font-weight: 600;
  word-break: break-all;
}

.ct-link:hover {
  color: var(--mo-p600);
}

.ct-cta {
  margin-top: var(--mo-sp-8);
  padding: var(--mo-sp-7) var(--mo-sp-5);
  text-align: center;
  border-radius: var(--mo-r-xl);
  background:
    radial-gradient(circle at 80% 18%, rgba(255, 255, 255, 0.14), transparent 40%),
    linear-gradient(135deg, var(--mo-p800), var(--mo-p600));
}

.ct-cta p {
  margin: 0 0 var(--mo-sp-4);
  color: var(--mo-n50);
  font-size: var(--mo-fs-md);
}

.ct-btn {
  display: inline-block;
  padding: 10px 26px;
  border-radius: var(--mo-r-md);
  background: var(--mo-n50);
  color: var(--mo-p700);
  font-weight: 600;
  text-decoration: none;
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.ct-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.22);
}

@media (max-width: 720px) {
  .ct-grid {
    grid-template-columns: 1fr;
  }
}
</style>
