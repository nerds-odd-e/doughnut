/**
 * @jest-environment jsdom
 */
import MindmapSector from "@/models/MindmapSector.ts";

describe("MindmapSector", () => {
  const head = new MindmapSector(0, 0, 0, Math.PI * 2)

  it("head note has 1 child", async () => {
    const child = head.getChildSector(1, 0).coord(150, 50, 1)
    expect(child.x).toBeCloseTo(-75, 0)
    expect(child.y).toBeCloseTo(-235, 5)
  });

  it("head note has 2 children, get 2nd", async () => {
    const child = head.getChildSector(2, 1).coord(150, 50, 1)
    expect(child.x).toBeCloseTo(-285, 0)
    expect(child.y).toBeCloseTo(-25, 0)
  });

  it("head note has 2 children, 2nd child has 100 children", async () => {
    const child = head.getChildSector(2, 1)
    const grandchild = child.getChildSector(100, 0).coord(150, 50, 1)
    expect(grandchild.x).toBeCloseTo(-223, -1)
    expect(grandchild.y).toBeCloseTo(-225, -1)
  });

  it("head note has 2 children, 2nd child has 100 children, last grandchild", async () => {
    const child = head.getChildSector(2, 1)
    const grandchild = child.getChildSector(100, 99).coord(150, 50, 1)
    expect(grandchild.x).toBeCloseTo(-223, -1)
    expect(grandchild.y).toBeCloseTo(175, -1)
  });

  it("a more vertical connection", async () => {
    const head = new MindmapSector(0, 0, Math.PI/10, 0)
    const child = head.getChildSector(1, 0)
    const connection = child.connection(150, 50, 1)
    expect(connection.y1).toBeCloseTo(25, -1)
    expect(connection.x1).toBeCloseTo(8, -1)
  });

  it("a more horizontal connection", async () => {
    const head = new MindmapSector(0, 0, Math.PI * 0.55, 0)
    const child = head.getChildSector(1, 0)
    const connection = child.connection(150, 50, 1)
    expect(connection.x1).toBeCloseTo(75, -1)
    expect(connection.y1).toBeCloseTo(-11, -1)
    expect(connection.x2).toBeCloseTo(132, -1)
    expect(connection.y2).toBeCloseTo(-20, -1)
  });

})
