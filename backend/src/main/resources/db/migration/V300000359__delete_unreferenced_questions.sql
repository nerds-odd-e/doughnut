DELETE FROM mcq
WHERE ${questioncleanup}
  AND NOT EXISTS (SELECT 1 FROM recall_prompt WHERE recall_prompt.mcq_id = mcq.id);
