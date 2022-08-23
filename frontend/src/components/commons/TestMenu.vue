<template>
  <nav class="vertical-menu">
    <div class="menu">
      <PopupButton title="Testability">
        <template #dialog_body>
          <h1>Testability</h1>
          <CheckInput
            scope-name="testability"
            v-model="featureToggle"
            field="featureToggle"
          />
          <TextInput
            scope-name="testability"
            v-model="randomSelector"
            field="randomSelector"
            hint="can be 'first' or 'last'"
            @blur="updateRandomSelector"
          />
        </template>
      </PopupButton>
      <div v-if="featureToggle" class="nav-item">
        <em class="nav-link btn-danger">Feature Toggle is On </em>
      </div>
    </div>
  </nav>
</template>

<script lang="ts">
import { defineComponent, PropType } from "vue";
import PopupButton from "./Popups/PopupButton.vue";
import CheckInput from "../form/CheckInput.vue";
import TextInput from "../form/TextInput.vue";
import useStoredLoadingApi from "../../managedApi/useStoredLoadingApi";

export default defineComponent({
  setup() {
    return useStoredLoadingApi();
  },
  props: {
    featureToggleValue: Boolean,
    user: Object as PropType<Generated.User>,
  },
  emits: ["featureToggle"],
  components: {
    PopupButton,
    CheckInput,
    TextInput,
  },
  data() {
    return {
      featureToggle: this.featureToggleValue,
      randomSelector: "",
    };
  },
  methods: {
    updateRandomSelector() {
      this.storedApi.testability.setRandomizer(this.randomSelector);
    },
  },
  watch: {
    featureToggle() {
      this.storedApi.testability.setFeatureToggle(this.featureToggle);
      this.$emit("featureToggle", this.featureToggle);
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

  .menu {
    pointer-events: initial;
    background-color: yellow;
  }
}
</style>
