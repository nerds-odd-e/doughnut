let idCounter = 1

const generateId = () => {
  idCounter += 1
  return idCounter as Donut.ID
}

export default generateId
