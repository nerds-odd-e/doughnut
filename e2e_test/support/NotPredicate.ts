import { Predicate } from '@anev/ts-mountebank'

interface PredicateJSON {
  not: unknown
}

interface PredicateWithToJSON extends Predicate {
  toJSON(): PredicateJSON
}

export class NotPredicate implements PredicateWithToJSON {
  Predicate: PredicateWithToJSON

  constructor(predicate: PredicateWithToJSON) {
    this.Predicate = predicate
  }

  toJSON(): PredicateJSON {
    return {
      not: this.Predicate.toJSON(),
    }
  }
}
