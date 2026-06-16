<template>
  <input
    type="submit"
    name="submit"
    value="Assimilate"
    :class="['daisy-btn daisy-btn-primary', sizeClass]"
    data-test="assimilate"
    :disabled="disabled || assimilateDisabled"
    @click="$emit('assimilate', false)"
  />
  <input
    v-if="showSkip && skippedForRecall"
    type="submit"
    name="revive"
    value="Revive"
    :class="['daisy-btn daisy-btn-secondary', sizeClass]"
    data-test="revive"
    :disabled="disabled"
    @click="$emit('revive')"
  />
  <input
    v-else-if="showSkip"
    type="submit"
    name="skip"
    value="Skip recall"
    :class="['daisy-btn daisy-btn-secondary', sizeClass]"
    :disabled="disabled"
    @click="$emit('assimilate', true)"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue"

export default defineComponent({
  props: {
    disabled: {
      type: Boolean,
      default: false,
    },
    assimilateDisabled: {
      type: Boolean,
      default: false,
    },
    size: {
      type: String as () => "default" | "sm",
      default: "default",
      validator: (value: string) => ["default", "sm"].includes(value),
    },
    showSkip: {
      type: Boolean,
      default: true,
    },
    skippedForRecall: {
      type: Boolean,
      default: false,
    },
  },
  emits: ["assimilate", "revive"],
  computed: {
    sizeClass(): string {
      return this.size === "sm" ? "daisy-btn-sm" : ""
    },
  },
})
</script>
