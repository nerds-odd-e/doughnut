import { Coord, StraightConnection, Vector } from "./MindmapUnits";

class MindmapMetrics {
    scale: number

    constructor(scale: number, boxWidth?: number, boxHeight?: number) {
      this.scale = scale
    }

    get boxWidth(): number {
        if (this.scale <=1) return 150
        if (this.scale <=2) return 200
        return 300
    }

    get boxHeight(): number {
        if (this.scale <=1) return 50
        if (this.scale <=2) return 100
        return 200
    }

    borderVector(vector: Vector): Vector {
        let dy = this.boxHeight / 2 * Math.sign(Math.sin(vector.angle))
        let dx = dy / Math.tan(vector.angle)
        if (Number.isNaN(dx) || Math.abs(dx) > this.boxWidth / 2) {
            dx = this.boxWidth / 2 * Math.sign(Math.cos(vector.angle))
            dy = dx * Math.tan(vector.angle)
        }
        return { x: Math.round(vector.x * this.scale + dx), y: Math.round(vector.y * this.scale + dy), angle: vector.angle}
    }

    straighConnection(conn: StraightConnection): StraightConnection {
        const p2p = { x1: conn.x1 * this.scale, y1: conn.y1 * this.scale, x2: conn.x2 * this.scale, y2: conn.y2 * this.scale}
        const dx = p2p.x2 - p2p.x1
        const dy = p2p.y2 - p2p.y1
        let boxDx; let boxDy
        if (Math.abs(dx * this.boxHeight) > Math.abs(this.boxWidth * dy)) {
            boxDx = this.boxWidth / 2 * (dx > 0 ? 1 : -1)
            boxDy = dy * boxDx / dx
        }
        else {
            boxDy = this.boxHeight / 2 * (dy > 0 ? 1 : -1)
            boxDx = dx * boxDy / dy
        }
        return { x1: p2p.x1 + boxDx, y1: p2p.y1 + boxDy, x2: p2p.x2 - boxDx, y2: p2p.y2 - boxDy}
    }

    coord(crd: Coord): Coord {
        return { x: crd.x * this.scale - this.boxWidth / 2, y: crd.y * this.scale - this.boxHeight / 2}
    }

    linkVectors(fromO: Vector, toO: Vector): string {
      const icon = 42
      const dist = 100 * this.scale
      const from: Vector = {
         x: Math.round(fromO.x + icon * Math.cos(fromO.angle)),
         y: Math.round(fromO.y + icon * Math.sin(fromO.angle)),
         angle: fromO.angle
      }
      const to: Vector = {
         x: Math.round(toO.x + icon * Math.cos(toO.angle)),
         y: Math.round(toO.y + icon * Math.sin(toO.angle)),
         angle: toO.angle
      }
      return `M ${ from. x} ${ from .y 
      } C ${ from.x + dist * Math.cos(from.angle)} ${ from.y + dist * Math.sin(from.angle)
      } ${ to.x + dist * Math.cos(to.angle)} ${ to.y + dist * Math.sin(to.angle)
      } ${ to.x } ${ to.y
      }`
    }


}

export default MindmapMetrics
