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

<script lang="ts">

interface Offset {
  x: number
  y: number
  scale: number
}

interface Position {
  x: number
  y: number
}

class Gesture {
  pointers: any
  startOffset: any

  constructor(startOffset: Offset, pointerId: number, start: Position) {
    this.startOffset = { ... startOffset }
    this.pointers = {}
    this.newPointer(pointerId, start)
  }

  newPointer(pointerId: number, start: Position): void {
    this.pointers[pointerId] = { start: start }
  }

  move(pointerId: number, pos: Position): void {
    this.pointers[pointerId].current = pos
  }

  get offset(): any {
    const pointer = this.pointers[Object.keys(this.pointers)[0]]
    return {
      x: this.startOffset.x + pointer.current.x - pointer.start.x,
      y: this.startOffset.y + pointer.current.y - pointer.start.y
    }
  }

}

export default {
  props: { modelValue: Object },
  emits: ['update:modelValue'],
  data() {
    return {
      gesture: null,
      pointers: {},
      startOffset: {},
    };
  },
  methods: {
    startDrag(e) {
      if (this.gesture) {
        this.pointers[e.pointerId] = { start: { x: e.clientX, y: e.clientY} }
        this.gesture.newPointer(e.pointerId, { x: e.clientX, y: e.clientY})
        return
      }
      this.gesture = new Gesture(this.modelValue, e.pointerId, { x: e.clientX, y: e.clientY})
      this.pointers = { [e.pointerId]: { start: { x: e.clientX, y: e.clientY}} }
      this.startOffset = {...this.modelValue}
    },
    onDrag(e) {
      if (this.gesture) {
        e = e.changedTouches ? e.changedTouches[0] : e;
        this.gesture!.move(e.pointerId, {x: e.clientX, y: e.clientY})
        const pointer = this.pointers[e.pointerId]
        pointer.current = { x: e.clientX, y: e.clientY}
        this.modelValue.x = this.gesture!.offset.x
        this.modelValue.y = this.gesture!.offset.y
        this.$emit("update:modelValue", this.modelValue)
      }
    },
    stopDrag() {
      this.gesture = null;
    },

    onZoom(e) {
      this.zoom(e.currentTarget.getBoundingClientRect(), {x: e.clientX, y: e.clientY}, e.deltaY * 0.01)
      this.$emit("update:modelValue", this.modelValue)
    },

    zoom(rect, focus, newScale) {
      const {width, height, top} = rect
      const oldScale = this.modelValue.scale
      const newOffset = (oldOffset, center, client) => {
        return (oldOffset + center - client) * this.modelValue.scale / oldScale - center + client
      }
      this.modelValue.scale += newScale
      if(this.modelValue.scale > 5) this.modelValue.scale = 5
      if(this.modelValue.scale < 0.1) this.modelValue.scale = 0.1
      this.modelValue.x = newOffset(this.modelValue.x, width / 2, focus.x)
      this.modelValue.y = newOffset(this.modelValue.y, height / 2, focus.y - top)
    },

  },
};
</script>
