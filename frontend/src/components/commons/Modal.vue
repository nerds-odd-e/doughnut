<template>
  <transition name="modal">
    <div class="modal-mask">
      <div class="modal-wrapper" @mousedown.self="$emit('close_request')">
        <div :class="sidebar ? 'modal-sidebar' : 'modal-container'">
          <button class="close-button" @click="$emit('close_request')">
            <SvgClose />
          </button>

          <div class="modal-header" v-if="$slots.header">
            <slot name="header" />
          </div>

          <div class="modal-body">
            <slot name="body" />
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import SvgClose from "../svgs/SvgClose.vue";

export default defineComponent({
  props: {
    sidebar: Boolean,
  },
  emits: ["close_request"],
  components: { SvgClose },
});
</script>

<style scoped>
.modal-mask {
  position: fixed;
  z-index: 9990;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: table;
  transition: opacity 0.3s ease;
}

.modal-wrapper {
  display: table-cell;
  vertical-align: middle;
}

.modal-container {
  position: relative;
  max-width: 700px;
  max-height: 100vh;
  overflow: auto;
  margin: 0px auto;
  padding: 20px 30px;
  background-color: #fff;
  border-radius: 2px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.33);
  transition: all 0.3s ease;
}

.modal-sidebar {
  position: relative;
  max-width: 300px;
  height: 100vh;
  overflow: auto;
  margin-left: 0px;
  padding: 5px 0px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.33);
  transition: all 0.3s ease;
}

.modal-header h3 {
  margin-top: 0;
  color: #42b983;
}

.modal-body {
  margin: 20px 0;
}

.close-button {
  position: absolute;
  right: 0.3em;
  top: 0.3em;
  width: 26px;
  padding: 1px;
  height: 26px;
  border: none;
  background: none;
}

/*
 * The following styles are auto-applied to elements with
 * transition="modal" when their visibility is toggled
 * by Vue.js.
 *
 * You can easily play with the modal transition by editing
 * these styles.
 */

.modal-enter {
  opacity: 0;
}

.modal-leave-active {
  opacity: 0;
}

.modal-enter .modal-container,
.modal-leave-active .modal-container {
  -webkit-transform: scale(1.1);
  transform: scale(1.1);
}
</style>
