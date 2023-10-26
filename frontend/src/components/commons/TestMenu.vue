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
          hint="can be 'first' or 'last'"
          @blur="updateRandomSelector"
        />
      </PopButton>
      <div v-if="featureToggle" class="nav-item">
        <em class="nav-link btn-danger">Feature Toggle is On </em>
      </div>
    </div>
  </nav>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import PopButton from "./Popups/PopButton.vue";
import CheckInput from "../form/CheckInput.vue";
import TextInput from "../form/TextInput.vue";
import useLoadingApi from "../../managedApi/useLoadingApi";

export default defineComponent({
  setup() {
    return useLoadingApi();
  },
  props: {
    featureToggle: Boolean,
    user: Object as PropType<Generated.User>,
  },
  emits: ["featureToggle"],
  components: {
    PopButton,
    CheckInput,
    TextInput,
  },
  data() {
    return {
      randomSelector: "",
    };
  },
  methods: {
    updateRandomSelector() {
      this.api.testability.setRandomizer(this.randomSelector);
    },
    updateFeatureToggle(value) {
      this.api.testability.setFeatureToggle(value);
      this.$emit("featureToggle", value);
    },
  },
});
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
