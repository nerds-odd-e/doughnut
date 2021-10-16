/**
 * @jest-environment jsdom
 */
import MindmapSector from "@/components/notes/MindmapSector.ts";

describe("MindmapSector", () => {
  const head = new MindmapSector(0, 0, 0, Math.PI * 2)

  it("head note has 1 child", async () => {
    const child = head.getChildSector(1, 0)
    expect(child.x).toBeCloseTo(0, 5)
    expect(child.y).toBeCloseTo(-210, 5)
  });

  it("head note has 2 children, get 2nd", async () => {
    const child = head.getChildSector(2, 1)
    expect(child.x).toBeCloseTo(-210, 5)
    expect(child.y).toBeCloseTo(0, 5)
  });

  it("head note has 2 children, 2nd child has 100 children", async () => {
    const child = head.getChildSector(2, 1)
    const grandchild = child.getChildSector(100, 0)
    expect(grandchild.x).toBeCloseTo(-148.882, 3)
    expect(grandchild.y).toBeCloseTo(-200.9095, 3)
  });

  it("head note has 2 children, 2nd child has 100 children, last grandchild", async () => {
    const child = head.getChildSector(2, 1)
    const grandchild = child.getChildSector(100, 99)
    expect(grandchild.x).toBeCloseTo(-148.882, 3)
    expect(grandchild.y).toBeCloseTo(200.9095, 3)
  });

})
