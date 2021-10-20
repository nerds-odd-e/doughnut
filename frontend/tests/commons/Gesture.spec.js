/**
 * @jest-environment jsdom
 */
import Gesture from "@/components/commons/Gesture";

describe("Gesture", () => {
  const frame = {width: 1000, height: 1000, top: 10}

  test("zoom out to 0.5", async () => {
    const gesture = new Gesture({x: 0, y: 0, scale: 1})
    gesture.newPointer(1, {x: 0, y: 0})
    gesture.newPointer(2, {x: 10, y: 0})
    gesture.move(frame, 2, {x: 5, y: 0})
    expect(gesture.offset.scale).toEqual(0.5)
  });
});

