import { Predicate } from "@anev/ts-mountebank"

export class NotPredicate implements Predicate {
  Predicate: Predicate

  constructor(predicate: Predicate) {
    this.Predicate = predicate
  }

  toJSON() {
    return {
      not: this.Predicate.toJSON(),
    }
  }
}
