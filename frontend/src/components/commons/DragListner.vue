import { PropType } from 'vue' import { PropType } from 'vue' import { PropType
} from 'vue' import { PropType } from 'vue'
<template>
  <div
    style="cursor: pointer"
    @pointerdown="startDrag"
    @pointermove.prevent="onDrag"
    @touchmove.prevent="onDrag"
    @pointerup="stopDrag"
    @pointercancel="stopDrag"
    @wheel.prevent="onZoom"
    @mousewheel.prevent="onZoom"
  >
    <slot />
  </div>
</template>

<script>
import Gesture from "./Gesture";

export default {
  props: { modelValue: { type: Object, required: true } },
  emits: ["update:modelValue"],
  data() {
    return {
      gesture: null,
    };
  },
  methods: {
    startDrag(e) {
      if (this.gesture == null) {
        this.gesture = new Gesture(this.modelValue);
      }
      this.gesture.newPointer(e.pointerId, { x: e.clientX, y: e.clientY });
    },
    onDrag(e) {
      if (!this.gesture) return;
      const event = e.changedTouches ? e.changedTouches[0] : e;
      this.gesture.move(
        event.currentTarget.getBoundingClientRect(),
        event.pointerId,
        {
          x: event.clientX,
          y: event.clientY,
        },
      );
      this.gesture.shiftDown(event.shiftKey);
      Object.assign(this.modelValue, this.gesture.offset);
      this.$emit("update:modelValue", this.modelValue);
    },
    stopDrag() {
      this.gesture = null;
    },

    onZoom(e) {
      this.gesture = new Gesture(this.modelValue);
      this.gesture.newPointer(e.pointerId, { x: e.clientX, y: e.clientY });
      Object.assign(
        this.modelValue,
        this.gesture.zoom(
          e.currentTarget.getBoundingClientRect(),
          this.modelValue.scale + e.deltaY * 0.01,
        ),
      );
      this.gesture = null;
      this.$emit("update:modelValue", this.modelValue);
    },
  },
};
</script>
