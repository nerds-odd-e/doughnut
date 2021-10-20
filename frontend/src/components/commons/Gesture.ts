import { PropType } from 'vue';

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

  constructor(startOffset: Offset) {
    this.startOffset = { ... startOffset }
    this.pointers = {}
  }

  reset(): void {
    this.pointers = {}
  }

  newPointer(pointerId: number, start: Position): void {
    this.pointers[pointerId] = { start }
  }

  move(pointerId: number, pos: Position): void {
    this.pointers[pointerId].current = pos
  }

  get isDragging(): any {
    return Object.keys(this.pointers).length > 0
  }

  get offset(): any {
    const pointer = this.pointers[Object.keys(this.pointers)[0]]
    return {
      x: this.startOffset.x + pointer.current.x - pointer.start.x,
      y: this.startOffset.y + pointer.current.y - pointer.start.y
    }
  }

  zoom(rect: any, newScale: number) {
    const pointer = this.pointers[Object.keys(this.pointers)[0]]
    const {width, height, top} = rect
    const adjustedNewScale = Math.max(0.1, Math.min(5, newScale, 5))

    const newOffset = (oldOffset: number, center: number, client: number) => (oldOffset + center - client) * adjustedNewScale / this.startOffset.scale - center + client

    return {
      scale: adjustedNewScale,
      x: newOffset(this.startOffset.x, width / 2, pointer.start.x),
      y: newOffset(this.startOffset.y, height / 2, pointer.start.y - top),
    }
  }

}

export default Gesture