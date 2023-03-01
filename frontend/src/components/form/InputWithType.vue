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
      <div class="error-msg" v-if="!!errors">{{ errors }}</div>
    </div>
  </div>
</template>

<script>
import { startCase, camelCase } from "lodash";

export default {
  props: {
    scopeName: String,
    field: String,
    title: String,
    hint: String,
    errors: Object,
    beforeLabel: { type: Boolean, default: false },
  },
  computed: {
    titlized() {
      return this.title ? this.title : startCase(camelCase(this.field));
    },
    controlId() {
      return `${this.scopeName}-${this.field}`;
    },
  },
};
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
