-- Supports capped structural-peer queries (same folder or notebook root) with id / seeded order.
CREATE INDEX `idx_note_structural_peer` ON `note` (`notebook_id`, `folder_id`, `deleted_at`, `id`);
