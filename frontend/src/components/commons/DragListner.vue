<template>
  <div style="cursor: pointer; overscroll-behavior: contain;"
    @mousedown="startDrag"
    @touchstart="startDrag"
    @mousemove="onDrag"
    @touchmove="onDrag"
    @mouseup="stopDrag"
    @touchend="stopDrag"
    @mousewheel="zoom"
  >
  <slot/>
  </div>
</template>

<script>
export default {
  props: { modelValue: Object },
  emits: ['update:modelValue'],
  data() {
    return {
      dragging: false,
      start: {},
      rand: 'not yet',
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
    },
    zoom(e) {
      const {width, height, top} = e.currentTarget.getBoundingClientRect()
      const { clientX, clientY } = e
      const oldScale = this.modelValue.scale
      const newOffset = (oldOffset, center, client) => {
        return (oldOffset + center - client) * this.modelValue.scale / oldScale - center + client
      }
      this.modelValue.scale += e.deltaY * 0.01
      if(this.modelValue.scale > 5) this.modelValue.scale = 5
      if(this.modelValue.scale < 0.1) this.modelValue.scale = 0.1
      this.modelValue.x = newOffset(this.modelValue.x, width / 2, clientX)
      this.modelValue.y = newOffset(this.modelValue.y, height / 2, clientY - top)
      this.$emit("update:modelValue", this.modelValue)
    },

  },
};
</script>
