<template>
  <template v-if="popupInfo">
    <Modal
      v-if="popupInfo.type === 'alert'"
      class="popups"
      @close_request="resolve(true)"
    >
      <template v-slot:header>
        <h2>Information</h2>
      </template>
      <template v-slot:body>
        <div>
          <span>{{ popupInfo.message }}</span>
        </div>
        <button class="btn btn-success" v-on:click="resolve(true)">OK</button>
      </template>
    </Modal>

    <Modal
      v-if="popupInfo.type === 'confirm'"
      class="popups"
      @close_request="resolve(false)"
    >
      <template v-slot:header>
        <h2>Please confirm</h2>
      </template>
      <template v-slot:body>
        <div>
          <span>{{ popupInfo.message }}</span>
        </div>
        <button class="btn btn-success" v-on:click="resolve(true)">OK</button>
        <button class="btn btn-secondary" v-on:click="resolve(false)">
          Cancel
        </button>
      </template>
    </Modal>

    <Modal
      v-if="popupInfo.type === 'dialog'"
      class="popups"
      @close_request="resolve(null)"
    >
      <template v-slot:body>
        <component v-if="popupInfo.slot" :is="popupInfo.slot"
        :doneHandler="resolve"
         />
        <component
        v-else
          :is="popupInfo.component"
          v-bind="popupInfo.attrs"
          @done="resolve($event)"
        />
      </template>
    </Modal>
  </template>
</template>

<script lang="ts">
import { defineComponent, } from "vue";
import Modal from "../Modal.vue";
import usePopups, {PopupInfo} from "./usePopup";

export default defineComponent({
  setup() {
    return usePopups();
  },
  emits: ["done"],
  components: { Modal },
  methods: {
    resolve(result: unknown) {
      this.popups.done(result)
    },
  },
  data() {
    return {
      popupData: {
        popupInfo: undefined
      }
    } as {
      popupData: {
        popupInfo?: PopupInfo
      }
    };
  },
  computed: {
    popupInfo() { return this.popupData.popupInfo }
  },
  mounted() {
    this.popups.register(this.popupData);

  }
});
</script>

<style lang="scss" scoped>
.popups {
  z-index: 9998;
}
</style>
