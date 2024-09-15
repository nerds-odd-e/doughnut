<template>
  <div class="form-group">
    <slot v-if="beforeLabel" />
    <label v-if="!!field || !!title" :for="controlId">
      {{ titlized }}
    </label>
    <i v-if="hint" class="hint" v-text="hint" />
    <div class="input-group">
      <template v-if="$slots.input_prepend">
        <div class="input-group-prepend">
          <slot name="input_prepend" />
        </div>
      </template>
      <slot v-if="!beforeLabel" />
      <div class="error-msg" v-if="!!errorMessage">{{ errorMessage }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue"
import { camelCase, startCase } from "lodash"

const props = defineProps({
  scopeName: String,
  field: String,
  title: String,
  hint: String,
  errorMessage: String,
  beforeLabel: { type: Boolean, default: false },
})

const titlized = computed(() =>
  props.title ? props.title : startCase(camelCase(props.field))
)

const controlId = computed(() => `${props.scopeName}-${props.field}`)
</script>

<style lang="sass" scoped>

.error-msg
    width: 100%
    margin-top: .25rem
    font-size: .875em
    color: #dc3545

.hint
  margin-left: 5px
  font-size: smaller
</style>
