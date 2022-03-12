import Links from "./Links";

interface LegacyNote {
  id: number,
  parentId: number | undefined
  links: Links | undefined
  childrenIds: number[]
}

export default LegacyNote