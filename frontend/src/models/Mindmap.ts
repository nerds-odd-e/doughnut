import MindmapSector from "./MindmapSector";
import { Coord, StraightConnection, Vector } from "./MindmapUnits";
import MindmapMetrics from "./MindmapMetrics";

class Mindmap {
  rootMindmapSector: MindmapSector;

  rootNoteId: Doughnut.ID;

  metrics: MindmapMetrics;

  constructor(
    scale: number,
    rootMindmapSector: MindmapSector,
    rootNoteId: Doughnut.ID,
  ) {
    this.rootMindmapSector = rootMindmapSector;
    this.rootNoteId = rootNoteId;
    this.metrics = new MindmapMetrics(scale);
  }

  coord(sector: MindmapSector): Coord {
    return this.metrics.coord(sector.coord);
  }

  size(): string {
    if (this.metrics.scale <= 1) return "small";
    if (this.metrics.scale <= 2) return "medium";
    return "large";
  }

  connectFromParent(sector: MindmapSector): StraightConnection {
    return this.metrics.straighConnection(sector.connectionFromParent);
  }

  outSlot(
    from: MindmapSector,
    connectorCount: number,
    connectorIndex: number,
  ): Vector {
    return this.metrics.borderVector(
      from.outSlot(connectorCount, connectorIndex),
    );
  }

  inSlot(
    from: MindmapSector,
    connectorCount: number,
    connectorIndex: number,
  ): Vector {
    return this.metrics.borderVector(
      from.inSlot(connectorCount, connectorIndex),
    );
  }
}

export default Mindmap;
