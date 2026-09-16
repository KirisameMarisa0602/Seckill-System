<script setup lang="ts">
/**
 * 页内结果条。不依赖 Element Plus 的 toast 传送门，Cursor 内置浏览器也能看见。
 */
import type { BannerKind } from '../feedback'

defineProps<{
  kind: BannerKind
  text: string
  closable?: boolean
}>()

defineEmits<{ close: [] }>()
</script>

<template>
  <div class="status-banner" :class="kind" role="alert">
    <span>{{ text }}</span>
    <button v-if="closable" class="status-banner-close" type="button" aria-label="关闭" @click="$emit('close')">
      ×
    </button>
  </div>
</template>
