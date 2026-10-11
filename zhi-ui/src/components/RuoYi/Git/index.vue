<template>
  <div>
    <svg-icon icon-class="github" @click="goto" />
  </div>
</template>

<script setup lang="ts">
const url = ref('https://gitee.com/nevell/zhienterprise.git')

function goto() {
  window.open(url.value)
}
</script>
