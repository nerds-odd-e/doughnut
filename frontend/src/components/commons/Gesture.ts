interface Offset {
  x: number;
  y: number;
  scale: number;
  rotate: number;
}

interface Position {
  x: number;
  y: number;
}

interface Pointer {
  start: Position;
  current: Position;
}

interface BoundaryRect {
  width: number;
  height: number;
  top: number;
  left: number;
}

class Gesture {
  pointers: Map<Doughnut.ID, Pointer>;

  startOffset: Offset;

  rect: BoundaryRect;

  isShiftDown: boolean;

  constructor(startOffset: Offset) {
    this.startOffset = { ...startOffset };
    this.pointers = new Map();
    this.isShiftDown = false;
    this.rect = { width: 0, height: 0, top: 0, left: 0 };
  }

  reset(): void {
    this.pointers = new Map();
  }

  newPointer(pointerId: Doughnut.ID, start: Position): void {
    this.pointers.set(pointerId, { start, current: start });
  }

  move(rect: BoundaryRect, pointerId: Doughnut.ID, pos: Position): void {
    this.rect = rect;
    const pointer = this.pointers.get(pointerId);
    if (pointer) {
      pointer.current = pos;
    }
  }

  shiftDown(value: boolean): void {
    this.isShiftDown = value;
  }

  private get averagePointer(): Pointer {
    const average = (fn: (v: Pointer) => number) => {
      const sum = this.virtualPointers.map(fn).reduce((a, b) => a + b);
      return sum / this.virtualPointers.length;
    };

    return {
      current: {
        x: average((p) => p.current.x),
        y: average((p) => p.current.y),
      },
      start: { x: average((p) => p.start.x), y: average((p) => p.start.y) },
    };
  }

  private get startCenter(): Position {
    const { width, height, top, left } = this.rect;
    return {
      x: this.startOffset.x + width / 2 + left,
      y: this.startOffset.y + height / 2 + top,
    };
  }

  private get virtualPointers(): Array<Pointer> {
    const opposite = (p: Position): Position => ({
      x: this.startCenter.x * 2 - p.x,
      y: this.startCenter.y * 2 - p.y,
    });

    const iterator = this.pointers.values();
    const pointer1 = iterator.next().value;
    if (this.isShiftDown) {
      const pointer2 = {
        start: opposite(pointer1.start),
        current: opposite(pointer1.current),
      };
      return [pointer1, pointer2];
    }
    return Array.from(this.pointers.values());
  }

  get offset(): Offset {
    let newScale = this.startOffset.scale;
    let newRotate = this.startOffset.rotate;

    const [pointer1, pointer2] = this.virtualPointers;
    if (pointer2) {
      const distance = (pos1: Position, pos2: Position): number =>
        ((pos1.x - pos2.x) ** 2 + (pos1.y - pos2.y) ** 2) ** 0.5;
      newScale *= distance(
        pointer1?.current as Position,
        pointer2?.current as Position
      );
      newScale /= distance(
        pointer1?.start as Position,
        pointer2?.start as Position
      );

      const atan2 = (pos1: Position, pos2: Position): number =>
        Math.atan2(pos1.y - pos2.y, pos1.x - pos2.x);

      newRotate +=
        atan2(pointer1?.current as Position, pointer2?.current as Position) -
        atan2(pointer1?.start as Position, pointer2?.start as Position);
    }

    const beforeScale = {
      x:
        this.startOffset.x +
        this.averagePointer.current.x -
        this.averagePointer.start.x,
      y:
        this.startOffset.y +
        this.averagePointer.current.y -
        this.averagePointer.start.y,
      scale: this.startOffset.scale,
      rotate: newRotate,
    };
    return this.scale(beforeScale, newScale);
  }

  zoom(rect: BoundaryRect, newScale: number) {
    this.rect = rect;
    return this.scale(this.startOffset, newScale);
  }

  private scale(fromOffset: Offset, newScale: number): Offset {
    if (fromOffset.scale === newScale) return fromOffset;
    const pointer: Pointer = this.averagePointer;
    const { width, height, top, left } = this.rect;
    const adjustedNewScale = Math.max(0.1, Math.min(5, newScale, 5));

    const newOffset = (oldOffset: number, center: number, client: number) =>
      ((oldOffset + center - client) * adjustedNewScale) / fromOffset.scale -
      center +
      client;

    return {
      scale: adjustedNewScale,
      x: newOffset(fromOffset.x, width / 2, pointer.start.x - left),
      y: newOffset(fromOffset.y, height / 2, pointer.start.y - top),
      rotate: fromOffset.rotate,
    };
  }
}

export default Gesture;
