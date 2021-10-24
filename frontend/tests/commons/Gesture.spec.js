/**
 * @jest-environment jsdom
 */
import Gesture from "@/components/commons/Gesture";
import { fromPairs } from "lodash";

describe("Gesture", () => {
  const top = 10
  const left = 0
  const frame = {width: 1000, height: 1000, top, left}

  test("zoom out to 0.5", async () => {
    const gesture = new Gesture({x: 0, y: 0, scale: 1})
    gesture.newPointer(1, {x: frame.width/2, y: 0})
    gesture.newPointer(2, {x: frame.width/2 + 10, y: 0})
    gesture.move(frame, 2, {x:  frame.width/2 + 5, y: 0})
    expect(gesture.offset.scale).toEqual(0.5)
    expect(gesture.offset.x).toEqual(1.25)
  });

  describe("rotate", () => {

    test("does not change offset", async () => {
      const gesture = new Gesture({x: 0, y: 0, scale: 1})
      gesture.newPointer(1, {x: frame.width/2, y: 0})
      gesture.move(frame, 1, {x:  frame.width/2 + 5, y: 0})
      gesture.shiftDown(true)
      expect(gesture.offset.x).toEqual(0)
    });

    test("change scale as comparing to center", async () => {
      const gesture = new Gesture({x: 0, y: 0, scale: 1})
      gesture.newPointer(1, {x: frame.width/2 + left, y: frame.height/2 + top + 10})
      gesture.move(frame, 1, {x:  frame.width/2 + left, y: frame.height/2 + top + 20})
      gesture.shiftDown(true)
      expect(gesture.offset.scale).toEqual(2)
      expect(gesture.offset.x).toEqual(0)
    });

  });

});

