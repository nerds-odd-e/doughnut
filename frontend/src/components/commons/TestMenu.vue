<template>
  <nav class="vertical-menu">
    <div class="menu">
      <PopButton title="Testability">
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
      <div v-if="featureToggle" class="nav-item">
        <em class="nav-link daisy-btn-danger">Feature Toggle is On </em>
      </div>
    </div>
  </nav>
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
.vertical-menu {
  pointer-events: none;
  position: fixed;
  right: 0px;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 60px;
  font-size: x-small;
  top: 0px;

  .menu {
    pointer-events: initial;
    background-color: yellow;
  }
}
</style>
