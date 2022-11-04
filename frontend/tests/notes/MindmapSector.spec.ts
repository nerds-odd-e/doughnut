import { describe, it, expect } from "vitest";
import Mindmap from "../../src/models/Mindmap";
import MindmapSector from "../../src/models/MindmapSector";

describe("MindmapSector", () => {
  let head = new MindmapSector(0, 0, 0, Math.PI * 2);
  const mindmap = new Mindmap(1, head, 0);

  it("head note has 1 child", async () => {
    const child = head.getChildSector(1, 0);
    const coord = mindmap.coord(child);
    expect(coord.x).toBeCloseTo(-285, 0);
    expect(coord.y).toBeCloseTo(-25, 5);
  });

  it("head note has 2 children, get 2nd", async () => {
    const child = head.getChildSector(2, 1);
    const coord = mindmap.coord(child);
    expect(coord.x).toBeCloseTo(-75, 0);
    expect(coord.y).toBeCloseTo(-235, 0);
  });

  it("head note has 2 children, 2nd child has 100 children", async () => {
    const child = head.getChildSector(2, 1);
    const grandchild = child.getChildSector(100, 0);
    const coord = mindmap.coord(grandchild);
    expect(coord.x).toBeCloseTo(-4093, -1);
    expect(coord.y).toBeCloseTo(987, -1);
  });

  it("head note has 2 children, 2nd child has 100 children, last grandchild", async () => {
    const child = head.getChildSector(2, 1);
    const grandchild = child.getChildSector(100, 99);
    const coord = mindmap.coord(grandchild);
    expect(coord.x).toBeCloseTo(3943, -1);
    expect(coord.y).toBeCloseTo(987, -1);
  });

  it("a more vertical connection", async () => {
    head = new MindmapSector(0, 0, Math.PI / 10, 0);
    const child = head.getChildSector(1, 0);
    const connection = mindmap.connectFromParent(child);
    expect(connection.y1).toBeCloseTo(25, -1);
    expect(connection.x1).toBeCloseTo(75, -1);
  });

  it("a more horizontal connection", async () => {
    head = new MindmapSector(0, 0, Math.PI * 0.55, 0);
    const child = head.getChildSector(1, 0);
    const connection = mindmap.connectFromParent(child);

    expect(connection.x1).toBeCloseTo(-3, -1);
    expect(connection.y1).toBeCloseTo(25, -1);
    expect(connection.x2).toBeCloseTo(-28, -1);
    expect(connection.y2).toBeCloseTo(182, -1);
  });
});
