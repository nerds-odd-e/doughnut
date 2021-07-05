<template>
<div class="form-group">
    <slot v-if="beforeLabel" />
    <label :for="`${scopeName}-${field}`">{{titlized}}</label>
    <slot v-if="!beforeLabel" />
    <div class="invalid-feedback" v-if="!!errors">{{errors}}</div>
</div>
</template>

<script>
  import {startCase, camelCase } from "lodash"
  export default {
    props: { scopeName: String, field: String, errors: Object, beforeLabel: {type: Boolean, default: false}},
    methods: {
      titlized() {
        return startCase(camelCase(this.field))
      }
    }

  }
</script>
