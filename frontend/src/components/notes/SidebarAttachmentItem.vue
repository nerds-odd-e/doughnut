<template>
  <li class="sidebar-attachment-li" role="treeitem" :aria-label="attachment.filename">
    <div class="attachment-row">
      <File class="shrink-0" :size="14" aria-hidden="true" />
      <span class="sidebar-attachment-label">{{ attachment.filename }}</span>
    </div>
  </li>
</template>

<script setup lang="ts">
import type { NotebookAttachment } from "@generated/donut-backend-api"
import { File } from "@lucide/vue"

defineProps<{ attachment: NotebookAttachment }>()
</script>

<style lang="scss" scoped>
.sidebar-attachment-li {
  list-style: none;
  width: 100%;
}

.attachment-row {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  width: 100%;
  min-height: 2rem;
  padding: 0.125rem 0.25rem 0.125rem 1.625rem;
  box-sizing: border-box;
}
</style>
