<template>
  <div class="testability-wrapper">
    <PopButton title="Testability" :btn-class="'testability-button'">
      <template #button_face>
        <span class="button-text">T</span>
      </template>
      <h1>Testability</h1>
      <CheckInput
        scope-name="testability"
        :model-value="featureToggle"
        @update:model-value="updateFeatureToggle"
        field="featureToggle"
      />
      <TextInput
        scope-name="testability"
        v-model="randomSelector"
        field="randomSelector"
        hint="can be 'seed', 'first' or 'last'"
        @blur="updateRandomSelector"
      />
      <TextInput
        scope-name="testability"
        v-model="seed"
        field="seed"
        hint="Only works when randomSelector is 'seed'"
        @blur="updateRandomSelector"
      />
    </PopButton>
  </div>
</template>

<script lang="ts">
import type { User } from "@generated/backend"
import type { Randomization } from "@generated/backend"
import { TestabilityRestController } from "@generated/backend/sdk.gen"
import type { PropType } from "vue"
import { defineComponent } from "vue"
import CheckInput from "../form/CheckInput.vue"
import TextInput from "../form/TextInput.vue"
import PopButton from "./Popups/PopButton.vue"

export default defineComponent({
  props: {
    featureToggle: Boolean,
    user: Object as PropType<User>,
  },
  emits: ["featureToggle"],
  components: {
    PopButton,
    CheckInput,
    TextInput,
  },
  data() {
    return {
      randomSelector: "seed" as Randomization["choose"],
      seed: 0,
    }
  },
  methods: {
    async updateRandomSelector() {
      await TestabilityRestController.randomizer({
        body: {
          choose: this.randomSelector,
          seed: this.seed,
        },
      })
    },
    async updateFeatureToggle(value) {
      await TestabilityRestController.enableFeatureToggle({
        body: {
          enabled: value,
        },
      })
      this.$emit("featureToggle", value)
    },
  },
})
</script>

<style lang="scss" scoped>
.testability-wrapper {
  position: fixed;
  bottom: 20px;
  left: 20px;
  z-index: 1000;
}

:deep(.testability-button) {
  position: static !important;
  width: 48px !important;
  height: 48px !important;
  border-radius: 50% !important;
  background-color: yellow !important;
  color: black !important;
  border: none !important;
  cursor: pointer !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2) !important;
  transition: background-color 0.3s !important;
  font-weight: bold !important;
  font-size: 20px !important;
  padding: 0 !important;
  margin: 0 !important;

  &:hover {
    background-color: #e6e600 !important;
  }

  .button-text {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
