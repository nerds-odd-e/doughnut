<template>
  <div
    class="daisy-tabs daisy-tabs-box bg-base-200 p-2 mb-6"
    :data-testid="`${testIdPrefix}-tabs`"
  >
    <a
      :class="tabClass('readme')"
      role="button"
      href="#"
      :data-testid="`${testIdPrefix}-tab-readme`"
      @click.prevent="model = 'readme'"
    >Readme</a>
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
export type WorkspaceReadmeSettingsTab = "readme" | "settings"

const model = defineModel<WorkspaceReadmeSettingsTab>({ required: true })

withDefaults(
  defineProps<{
    testIdPrefix?: string
  }>(),
  {
    testIdPrefix: "workspace",
  }
)

const tabClass = (tab: WorkspaceReadmeSettingsTab) =>
  `daisy-tab daisy-tab-lg ${model.value === tab ? "daisy-tab-active" : ""}`
</script>
