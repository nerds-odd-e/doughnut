/// <reference types="cypress" />
/// <reference types="../support" />
// @ts-check

import { When } from "@badeball/cypress-cucumber-preprocessor"
import pageObjects from "../page_objects"

When("I input the ask statement {string}", (noteTitle: string) => {
  pageObjects.askQuestionForNote(noteTitle)
})
