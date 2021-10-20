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

interface Pointer {
  start: Position
  current: Position
}

class Gesture {
  pointers: Map<number, Pointer>

  startOffset: any

  constructor(startOffset: Offset) {
    this.startOffset = { ... startOffset }
    this.pointers = new Map
  }

  reset(): void {
    this.pointers = new Map
  }

  newPointer(pointerId: number, start: Position): void {
    this.pointers.set(pointerId, { start, current: start })
  }

  move(rect: any, pointerId: number, pos: Position): void {
    this.pointers.get(pointerId)!.current = pos
  }

  get offset(): Offset {
    const pointer: Pointer = this.pointers.values().next().value
    let newScale = this.startOffset.scale

    if(this.pointers.size > 1) {
      const iterator = this.pointers.values()
      const p1 = iterator.next().value
      const p2 = iterator.next().value
      const distance = (pos1: Position, pos2: Position): number =>
        ((pos1.x - pos2.x)**2 + (pos1.y - pos2.y)**2) ** .5
      newScale *= distance(p1.current, p2.current)
      newScale /= distance(p1.start, p2.start)
    }

    return {
      x: this.startOffset.x + pointer.current.x - pointer.start.x,
      y: this.startOffset.y + pointer.current.y - pointer.start.y,
      scale: newScale
    }
  }

  zoom(rect: any, newScale: number) {
    const pointer: Pointer = this.pointers.values().next().value
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