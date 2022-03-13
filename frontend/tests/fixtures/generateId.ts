import Doughnut from "../../src/@types/Doughnut";

let idCounter = 1;

const generateId = () => {
  idCounter += 1;
  return idCounter as Doughnut.ID;
}

export default generateId

