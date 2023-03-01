<template>
  <div class="form-group">
    <slot v-if="beforeLabel" />
    <label v-if="!!field || !!title" :for="controlId">
      <slot name="label_content" />
      <template v-if="!$slots.label_content">
        {{ titlized }}
      </template>
    </label>
    <i v-if="hint" class="hint" v-text="hint" />
    <slot v-if="!beforeLabel" />
    <div class="error-msg" v-if="!!errors">{{ errors }}</div>
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
