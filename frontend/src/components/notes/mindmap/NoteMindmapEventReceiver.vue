<template>
  <div
    @mousedown="startDrag"
    @touchstart="startDrag"
    @mousemove="onDrag"
    @touchmove="onDrag"
    @mouseup="stopDrag"
    @touchend="stopDrag"
  />
</template>

<script>
export default {
  props: { modelValue: Object },
  emits: ['update:modelValue'],
  data() {
    return {
      dragging: false,
      start: {},
    };
  },
  methods: {
    startDrag(e) {
      e = e.changedTouches ? e.changedTouches[0] : e;
      this.dragging = true;
      this.start.x = e.clientX;
      this.start.y = e.clientY;
      this.start.offset = {...this.modelValue}
    },
    onDrag(e) {
      if (this.dragging) {
        e = e.changedTouches ? e.changedTouches[0] : e;
        this.modelValue.x = this.start.offset.x + e.clientX - this.start.x;
        this.modelValue.y = this.start.offset.y + e.clientY - this.start.y;
        this.$emit("update:modelValue", this.modelValue)
      }
    },
    stopDrag() {
      this.dragging = false;
    }
  },
};
</script>