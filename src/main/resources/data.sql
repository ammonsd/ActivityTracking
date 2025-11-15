-- Insert dropdown values (skip if already exists)
-- Structure: category=TASK, subcategory=CLIENT/PROJECT/PHASE
-- This allows for future EXPENSE category with its own subcategories
INSERT INTO public.dropdownvalues 
(category, subcategory, itemvalue, displayorder, isactive)
VALUES 
  -- TASK -> PHASE subcategory
  ('TASK', 'PHASE', 'Development', 1, true),
  ('TASK', 'PHASE', 'Holiday', 2, true),
  ('TASK', 'PHASE', 'Meeting', 3, true),
  ('TASK', 'PHASE', 'Miscellaneous', 4, true),
  ('TASK', 'PHASE', 'Code Review', 5, true),
  ('TASK', 'PHASE', 'PTO', 6, true),
  ('TASK', 'PHASE', 'Training', 7, true),
  -- TASK -> CLIENT subcategory
  ('TASK', 'CLIENT', 'Corporate', 1, true),
  -- TASK -> PROJECT subcategory
  ('TASK', 'PROJECT', 'General Administration', 1, true),
  ('TASK', 'PROJECT', 'Non-Billable', 2, true)
ON CONFLICT (category, subcategory, itemvalue) DO NOTHING;
