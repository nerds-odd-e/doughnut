-- A learner can exclude an answered attempt as an ambiguous question: the attempt keeps its
-- original mark and records when it was excluded and why.
ALTER TABLE answer
  ADD COLUMN excluded_at timestamp NULL DEFAULT NULL,
  ADD COLUMN exclusion_reason text;
