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

<script>
export default {
  props: { modelValue: Object },
  emits: ['update:modelValue'],
  data() {
    return {
      dragging: false,
      pointers: {},
      startOffset: {},
    };
  },
  methods: {
    startDrag(e) {
      if (this.dragging) {
        this.pointers[e.pointerId] = { start: { x: e.clientX, y: e.clientY} }
        return
      }
      this.dragging = true;
      this.pointers = { [e.pointerId]: { start: { x: e.clientX, y: e.clientY}} }
      this.startOffset = {...this.modelValue}
    },
    onDrag(e) {
      if (this.dragging) {
        e = e.changedTouches ? e.changedTouches[0] : e;
        const pointer = this.pointers[e.pointerId]
        pointer.current = { x: e.clientX, y: e.clientY}
        this.modelValue.x = this.startOffset.x + pointer.current.x - pointer.start.x;
        this.modelValue.y = this.startOffset.y + pointer.current.y - pointer.start.y;
        this.$emit("update:modelValue", this.modelValue)
      }
    },
    stopDrag() {
      this.dragging = false;
    },

    onZoom(e) {
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
    },

  },
};
</script>
