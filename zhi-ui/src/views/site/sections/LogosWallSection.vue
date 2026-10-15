<template>
  <section class="pad surface">
    <div class="eh-wrap">
      <div class="tpl-sec-head rv">
        <div class="sec-tag">客户与案例</div>
        <h2>{{ cfg.title || '他们已经在用' }}</h2>
      </div>
      <div class="logos rv">
        <div v-for="name in partnerNames" :key="name" class="logocell">{{ name }}</div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getFrontFriendLinkList } from '@/api/site/friendLink'

/** config.count 控制 Logo 墙格数（默认 8）；数据来源为友链，不足回退占位 */
const props = defineProps<{ config?: Record<string, any> }>()

const count = computed(() => Number(props.config?.count) || 8)
const cfg = computed(() => props.config ?? {})
const partnerNames = ref<string[]>([])

const FALLBACK_PARTNERS = [
  '客户 A',
  '客户 B',
  '客户 C',
  '客户 D',
  '客户 E',
  '客户 F',
  '客户 G',
  '客户 H'
]

onMounted(async () => {
  try {
    const raw = await getFrontFriendLinkList()
    const list: unknown = Array.isArray(raw)
      ? raw
      : ((raw as { data?: unknown[]; rows?: unknown[] })?.data ??
        (raw as { data?: unknown[]; rows?: unknown[] })?.rows ??
        [])
    const names = (Array.isArray(list) ? list : [])
      .map((item: unknown) => String((item as { name?: unknown })?.name ?? '').trim())
      .filter(Boolean)
      .slice(0, count.value)
    partnerNames.value =
      names.length > 0
        ? [...names, ...FALLBACK_PARTNERS.slice(names.length)].slice(0, count.value)
        : FALLBACK_PARTNERS.slice(0, count.value)
  } catch {
    partnerNames.value = FALLBACK_PARTNERS.slice(0, count.value)
  }
})
</script>
