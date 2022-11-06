import Gesture from "../../src/components/commons/Gesture";

describe("Gesture", () => {
  const top = 10;
  const left = 0;
  const frame = { width: 1000, height: 1000, top, left };

  it("zoom out to 0.5", async () => {
    const gesture = new Gesture({ x: 0, y: 0, scale: 1, rotate: 0 });
    gesture.newPointer(1, { x: frame.width / 2, y: 0 });
    gesture.newPointer(2, { x: frame.width / 2 + 10, y: 0 });
    gesture.move(frame, 2, { x: frame.width / 2 + 5, y: 0 });
    expect(gesture.offset.scale).toEqual(0.5);
    expect(gesture.offset.x).toEqual(1.25);
  });

  describe("rotate and scale (holding shift)", () => {
    const holdingShiftMoveFromTo = (from, to) => {
      const gesture = new Gesture({ x: 0, y: 0, scale: 1, rotate: 0 });
      gesture.newPointer(1, {
        x: frame.width / 2 + left + from.x,
        y: frame.height / 2 + top + from.y,
      });
      gesture.move(frame, 1, {
        x: frame.width / 2 + left + to.x,
        y: frame.height / 2 + top + to.y,
      });
      gesture.shiftDown(true);
      return gesture;
    };

    it("change scale as comparing to center", async () => {
      const gesture = holdingShiftMoveFromTo({ x: 0, y: 10 }, { x: 0, y: 20 });
      expect(gesture.offset.scale).toEqual(2);
      expect(gesture.offset.x).toEqual(0);
      expect(gesture.offset.y).toEqual(0);
      expect(gesture.offset.rotate).toEqual(0);
    });

    it("rotate by center", async () => {
      const gesture = holdingShiftMoveFromTo({ x: 10, y: 10 }, { x: 10, y: 0 });
      expect(gesture.offset.x).toEqual(0);
      expect(gesture.offset.y).toEqual(0);
      expect(gesture.offset.rotate).toEqual(-Math.PI / 4);
    });
  });
});
