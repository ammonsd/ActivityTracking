-- Insert dropdown values (skip if already exists)
INSERT INTO public.dropdownvalues 
(category, subcategory, itemvalue, displayorder, isactive)
VALUES 
  ('PHASE', 'TASK', 'Development', 1, true),
  ('PHASE', 'TASK', 'Holiday', 2, true),
  ('PHASE', 'TASK', 'Meeting', 3, true),
  ('PHASE', 'TASK', 'Miscellaneous', 4, true),
  ('PHASE', 'TASK', 'Code Review', 5, true),
  ('PHASE', 'TASK', 'PTO', 6, true),
  ('PHASE', 'TASK', 'Training', 7, true),
  ('CLIENT', 'TASK', 'Corporate', 1, true),
  ('PROJECT', 'TASK', 'General Administration', 1, true)
ON CONFLICT (category, subcategory, itemvalue) DO NOTHING;
