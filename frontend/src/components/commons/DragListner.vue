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

  zoom(rect, newScale) {
    const pointer = this.pointers[Object.keys(this.pointers)[0]]
    const {width, height, top} = rect
    const newOffset = (oldOffset, center, client) => {
      return (oldOffset + center - client) * newScale / this.startOffset.scale - center + client
    }
    if(newScale > 5) newScale = 5
    if(newScale < 0.1) newScale = 0.1

    return {
      scale: newScale,
      x: newOffset(this.startOffset.x, width / 2, pointer.start.x),
      y: newOffset(this.startOffset.y, height / 2, pointer.start.y - top),
    }
  }

}

export default {
  props: { modelValue: Object },
  emits: ['update:modelValue'],
  data() {
    return {
      gesture: null,
      startOffset: {},
    };
  },
  methods: {
    startDrag(e) {
      if (this.gesture) {
        this.gesture.newPointer(e.pointerId, { x: e.clientX, y: e.clientY})
        return
      }
      this.gesture = new Gesture(this.modelValue, e.pointerId, { x: e.clientX, y: e.clientY})
    },
    onDrag(e) {
      if (this.gesture) {
        e = e.changedTouches ? e.changedTouches[0] : e;
        this.gesture!.move(e.pointerId, {x: e.clientX, y: e.clientY})
        Object.assign(this.modelValue, this.gesture!.offset)
        this.$emit("update:modelValue", this.modelValue)
      }
    },
    stopDrag() {
      this.gesture = null;
    },

    onZoom(e) {
      this.gesture = new Gesture(this.modelValue, e.pointerId, { x: e.clientX, y: e.clientY})
      Object.assign(this.modelValue, this.gesture.zoom(e.currentTarget.getBoundingClientRect(), this.modelValue.scale + e.deltaY * 0.01))
      this.gesture = null
      this.$emit("update:modelValue", this.modelValue)
    },

  },

};
</script>
