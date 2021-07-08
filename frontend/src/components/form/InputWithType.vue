<template>
<div class="form-group">
    <slot v-if="beforeLabel" />
    <label v-if="!!field" :for="controlId">{{titlized}}</label>
    <slot v-if="!beforeLabel" />
    <div class="invalid-feedback" v-if="!!errors">{{errors}}</div>
</div>
</template>

<script>
  import {startCase, camelCase } from "lodash"
  export default {
    props: {
       scopeName: String,
       field: String,
       errors: Object,
       beforeLabel: {type: Boolean, default: false},
    },
    computed: {
      titlized() {
        return startCase(camelCase(this.field))
      },
      controlId() {
        return `${this.scopeName}-${this.field}`
      }
    }

  }
</script>
