<template>
  <section class="pad surface">
    <div class="eh-wrap faq-wrap">
      <div class="tpl-sec-head rv">
        <div class="sec-tag">常见问题</div>
        <h2>{{ cfg.title || '你可能想问' }}</h2>
      </div>
      <div class="ct-faq rv d1">
        <div v-for="(f, fi) in items" :key="f.q" class="ct-faq-item">
          <button
            type="button"
            class="ct-faq-q"
            :aria-expanded="openFaq === fi"
            @click="openFaq = openFaq === fi ? -1 : fi"
          >
            {{ f.q }}
            <svg
              width="14"
              height="14"
              viewBox="0 0 14 14"
              fill="none"
              aria-hidden="true"
              :style="{ transform: openFaq === fi ? 'rotate(180deg)' : 'none' }"
            >
              <path
                d="M3 5l4 4 4-4"
                stroke="currentColor"
                stroke-width="1.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
          <p v-show="openFaq === fi" class="ct-faq-a">{{ f.a }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useSiteSettingsStore } from '@/stores/siteSettings'
import { getSiteFaqs } from '@/utils/siteContent'
import { setJsonLd } from '@/utils/jsonld'

const props = defineProps<{ config?: Record<string, any> }>()

const siteSettingsStore = useSiteSettingsStore()
const settings = computed(() => siteSettingsStore.siteSettings)
const cfg = computed(() => props.config ?? {})

const items = computed(() => {
  const fromConfig = props.config?.items
  return Array.isArray(fromConfig) && fromConfig.length > 0
    ? fromConfig
    : getSiteFaqs(settings.value?.site_faq)
})

const openFaq = ref(-1)

// FAQPage 结构化数据（方案文档 §6.2）
const updateJsonLd = () => {
  setJsonLd('zh-jsonld-faqpage', {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: items.value.map(f => ({
      '@type': 'Question',
      name: f.q,
      acceptedAnswer: { '@type': 'Answer', text: f.a }
    }))
  })
}

watch(items, updateJsonLd, { immediate: true })
</script>
