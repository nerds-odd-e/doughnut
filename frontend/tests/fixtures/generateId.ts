let idCounter = 1;

const generateId = () => {
  idCounter += 1;
  return idCounter;
}

export default generateId

