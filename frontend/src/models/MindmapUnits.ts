interface Vector {
  x: number;

  y: number;

  angle: number;
}

interface Coord {
  x: number;

  y: number;
}

interface StraightConnection {
  x1: number;

  y1: number;

  x2: number;

  y2: number;
}

export type { Vector, StraightConnection, Coord };
