import { PropType } from 'vue'
import { PropType } from 'vue'
import { PropType } from 'vue'
import { PropType } from 'vue'
<template>
  <div style="cursor: pointer"
    @pointerdown="startDrag"
    @pointermove.prevent="onDrag"
    @touchmove.prevent="onDrag"
    @pointerup="stopDrag"
    @pointercancel="stopDrag"
    @mousewheel="onZoom"
  >
  <slot/>
  </div>
</template>

<script >

import Gesture from './Gesture';


export default {
  props: { modelValue: { type: Object, required: true } },
  emits: ['update:modelValue'],
  data() {
    return {
      gesture: new Gesture(this.modelValue),
    };
  },
  methods: {
    startDrag(e) {
      this.gesture.newPointer(e.pointerId, { x: e.clientX, y: e.clientY})
    },
    onDrag(e) {
      if (!this.gesture.isDragging) return
      e = e.changedTouches ? e.changedTouches[0] : e;
      this.gesture.move(e.pointerId, {x: e.clientX, y: e.clientY})
      Object.assign(this.modelValue, this.gesture.offset)
      this.$emit("update:modelValue", this.modelValue)
    },
    stopDrag() {
      this.gesture.reset()
    },

    onZoom(e) {
      this.gesture.newPointer(e.pointerId, { x: e.clientX, y: e.clientY})
      Object.assign(
        this.modelValue,
        this.gesture.zoom(
          e.currentTarget.getBoundingClientRect(),
          this.modelValue.scale + e.deltaY * 0.01
        )
      )
      this.gesture.reset()
      this.$emit("update:modelValue", this.modelValue)
    },

  },

};
</script>
