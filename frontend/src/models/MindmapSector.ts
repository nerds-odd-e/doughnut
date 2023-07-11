import { Coord, StraightConnection, Vector } from "./MindmapUnits";

const splitAngle = (
  startAngle: number,
  angleRange: number,
  siblingCount: number,
  index: number,
): number =>
  startAngle +
  (angleRange / siblingCount) * index +
  angleRange / siblingCount / 2;

class MindmapSector {
  readonly radius = 210;

  parentX?: number;

  parentY?: number;

  nx: number;

  ny: number;

  startAngle: number;

  angleRange: number;

  constructor(x: number, y: number, startAngle: number, angleRange: number) {
    this.nx = x;
    this.ny = y;
    this.startAngle = startAngle;
    this.angleRange = angleRange;
  }

  get isHead(): boolean {
    return this.parentX === undefined;
  }

  get connectionFromParent(): StraightConnection {
    if (!this.parentX || !this.parentY) {
      return { x1: 0, y1: 0, x2: this.nx, y2: this.ny };
    }
    return { x1: this.parentX, y1: this.parentY, x2: this.nx, y2: this.ny };
  }

  get coord(): Coord {
    return { x: this.nx, y: this.ny };
  }

  getChildSector(siblingCount = 0, index = 0, extraScale = 1): MindmapSector {
    const radius = (this.radius * Math.max(5, siblingCount)) / 5;
    const ang = splitAngle(
      this.startAngle,
      this.angleRange,
      siblingCount,
      index,
    );
    const x = this.nx + radius * Math.cos(ang) * extraScale;
    const y = this.ny + radius * Math.sin(ang) * extraScale;
    const start =
      this.startAngle + (this.angleRange / siblingCount) * index - Math.PI / 10;
    const child = new MindmapSector(
      x,
      y,
      start,
      this.angleRange / siblingCount + Math.PI / 5,
    );
    child.parentX = this.nx;
    child.parentY = this.ny;
    return child;
  }

  outSlot(connectorCount: number, connectorIndex: number): Vector {
    const start = this.startAngle + this.angleRange / 2 + Math.PI / 2;
    const range = Math.PI;
    return this.connectPoint(start, range, connectorCount, connectorIndex);
  }

  inSlot(connectorCount: number, connectorIndex: number): Vector {
    const start = this.startAngle + this.angleRange / 2 - Math.PI / 2;
    const range = Math.PI;
    return this.connectPoint(start, range, connectorCount, connectorIndex);
  }

  private connectPoint(
    start: number,
    range: number,
    connectorCount: number,
    connectorIndex: number,
  ): Vector {
    const angle = splitAngle(start, range, connectorCount, connectorIndex);
    return { angle, x: this.nx, y: this.ny };
  }
}

export default MindmapSector;
