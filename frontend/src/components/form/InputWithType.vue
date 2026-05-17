<template>
  <div class="daisy-form-control">
    <div v-if="beforeLabel" class="flex items-center gap-2">
      <slot />
      <label
        v-if="showLabel"
        :for="controlId"
        class="daisy-label"
      >
        {{ titlized }}
      </label>
    </div>
    <template v-else>
      <label v-if="showLabel" :for="controlId" class="daisy-label">
        {{ titlized }}
      </label>
      <i v-if="hint" class="hint" v-text="hint" />
      <div class="daisy-join w-full items-stretch">
        <template v-if="$slots.input_prepend">
          <div class="daisy-join-item flex h-full items-stretch">
            <slot name="input_prepend" />
          </div>
        </template>
        <div v-if="$slots.input_prepend || $slots.input_append" class="daisy-join-item flex-1">
          <slot />
        </div>
        <template v-else>
          <slot />
        </template>
        <template v-if="$slots.input_append">
          <div class="daisy-join-item flex h-full items-stretch">
            <slot name="input_append" />
          </div>
        </template>
      </div>
    </template>
    <div class="text-error text-sm" v-if="!!errorMessage">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { camelCase, capitalize, words } from "es-toolkit"

const props = defineProps({
  scopeName: String,
  field: String,
  title: String,
  hint: String,
  errorMessage: String,
  beforeLabel: { type: Boolean, default: false },
  hideLabel: { type: Boolean, default: false },
})

const startCase = (str: string) => words(str).map(capitalize).join(" ")

const titlized = computed(() =>
  props.title ? props.title : startCase(camelCase(props.field ?? ""))
)

const showLabel = computed(
  () => !props.hideLabel && (!!props.field || !!props.title)
)

const controlId = computed(() => `${props.scopeName}-${props.field}`)
</script>

<style lang="sass" scoped>
.hint
  margin-left: 5px
  font-size: smaller
</style>
