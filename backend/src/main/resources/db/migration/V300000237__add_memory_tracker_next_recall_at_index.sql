-- Speed due-item lookups: filter by user_id and range/order by next_recall_at.
ALTER TABLE `memory_tracker`
  ADD KEY `idx_memory_tracker_user_next_recall_at` (`user_id`, `next_recall_at`);
