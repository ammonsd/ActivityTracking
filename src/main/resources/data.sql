-- Insert dropdown values (skip if already exists)
INSERT INTO public.dropdownvalues 
(category, itemvalue, displayorder, isactive)
VALUES 
  ('PHASE', 'Development', 1, true),
  ('PHASE', 'Holiday', 2, true),
  ('PHASE', 'Meeting', 3, true),
  ('PHASE', 'Miscellaneous', 4, true),
  ('PHASE', 'Code Review', 5, true),
  ('PHASE', 'PTO', 6, true),
  ('PHASE', 'Training', 7, true),
  ('CLIENT', 'Corporate', 1, true),
  ('PROJECT', 'General Administration', 1, true)
ON CONFLICT (category, itemvalue) DO NOTHING;
