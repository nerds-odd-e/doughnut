<template>
  <div class="daisy-form-control">
    <div v-if="beforeLabel" class="daisy-flex daisy-items-center daisy-gap-2">
      <slot />
      <label v-if="!!field || !!title" :for="controlId" class="daisy-label">
        {{ titlized }}
      </label>
    </div>
    <template v-else>
      <label v-if="!!field || !!title" :for="controlId" class="daisy-label">
        {{ titlized }}
      </label>
      <i v-if="hint" class="hint" v-text="hint" />
      <div class="daisy-join daisy-w-full">
        <template v-if="$slots.input_prepend">
          <div class="daisy-join-item">
            <slot name="input_prepend" />
          </div>
        </template>
        <div v-if="$slots.input_prepend || $slots.input_append" class="daisy-join-item daisy-flex-1">
          <slot />
        </div>
        <template v-else>
          <slot />
        </template>
        <template v-if="$slots.input_append">
          <div class="daisy-join-item">
            <slot name="input_append" />
          </div>
        </template>
      </div>
    </template>
    <div class="daisy-text-error daisy-text-sm" v-if="!!errorMessage">{{ errorMessage }}</div>
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
})

const startCase = (str: string) => words(str).map(capitalize).join(" ")

const titlized = computed(() =>
  props.title ? props.title : startCase(camelCase(props.field ?? ""))
)

const controlId = computed(() => `${props.scopeName}-${props.field}`)
</script>

<style lang="sass" scoped>
.hint
  margin-left: 5px
  font-size: smaller
</style>
