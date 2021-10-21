<template>
  <div class="form-group">
    <slot v-if="beforeLabel" />
    <label v-if="!!field" :for="controlId">{{ titlized }}</label>
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
    errors: Object,
    beforeLabel: { type: Boolean, default: false },
  },
  computed: {
    titlized() {
      return startCase(camelCase(this.field));
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
</style>