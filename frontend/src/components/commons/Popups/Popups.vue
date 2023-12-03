<template>
  <template v-for="(popupInfo, _index) in popupInfos" :key="_index">
    <Modal
      v-if="popupInfo.type === 'alert'"
      class="popups"
      @close_request="resolve(true)"
    >
      <template #header>
        <h2>Information</h2>
      </template>
      <template #body>
        <div>
          <pre style="white-space: pre-wrap">{{ popupInfo.message }}</pre>
        </div>
        <button class="btn btn-success" @click="resolve(true)">OK</button>
      </template>
    </Modal>

    <Modal
      v-if="popupInfo.type === 'confirm'"
      class="popups"
      @close_request="resolve(false)"
    >
      <template #header>
        <h2>Please confirm</h2>
      </template>
      <template #body>
        <div>
          <span>{{ popupInfo.message }}</span>
        </div>
        <button class="btn btn-success" @click="resolve(true)">OK</button>
        <button class="btn btn-secondary" @click="resolve(false)">
          Cancel
        </button>
      </template>
    </Modal>
  </template>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Modal from "../Modal.vue";
import usePopups, { PopupInfo } from "./usePopups";

export default defineComponent({
  setup() {
    return usePopups();
  },
  emits: ["done"],
  components: { Modal },
  methods: {
    resolve(result: unknown) {
      this.popups.done(result);
    },
  },
  data() {
    return {
      popupData: {
        popupInfo: [],
      },
    } as {
      popupData: {
        popupInfo: PopupInfo[];
      };
    };
  },
  computed: {
    popupInfos() {
      return this.popupData.popupInfo;
    },
  },
  mounted() {
    this.popups.register(this.popupData);
  },
});
</script>

<style lang="scss" scoped>
.popups {
  z-index: 9998;
}
</style>
