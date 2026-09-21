-- Expand step for retiring the legacy bundle column: accepted history now lives in
-- notebook_git_accepted_object, so a binding no longer has to carry a bundle. Existing values are
-- kept unchanged; running code still writes the column until a later release stops doing so.
ALTER TABLE `notebook_git_binding` MODIFY `bundle_bytes` longblob NULL;
