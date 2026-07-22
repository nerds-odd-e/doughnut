<template>
  <div
    class="daisy-tabs daisy-tabs-box bg-base-200 p-2 mb-6"
    :data-testid="`${testIdPrefix}-tabs`"
  >
    <a
      :class="tabClass('index')"
      role="button"
      href="#"
      :data-testid="`${testIdPrefix}-tab-index`"
      @click.prevent="model = 'index'"
    >Index</a>
    <a
      :class="tabClass('settings')"
      role="button"
      href="#"
      :data-testid="`${testIdPrefix}-tab-settings`"
      @click.prevent="model = 'settings'"
    >Settings</a>
  </div>
</template>

<script setup lang="ts">
export type WorkspaceIndexSettingsTab = "index" | "settings"

const model = defineModel<WorkspaceIndexSettingsTab>({ required: true })

withDefaults(
  defineProps<{
    testIdPrefix?: string
  }>(),
  {
    testIdPrefix: "workspace",
  }
)

const tabClass = (tab: WorkspaceIndexSettingsTab) =>
  `daisy-tab daisy-tab-lg ${model.value === tab ? "daisy-tab-active" : ""}`
</script>
