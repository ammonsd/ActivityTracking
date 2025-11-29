-- Add missing EXPENSE dropdown values for VENDOR and RECEIPT_STATUS subcategories

INSERT INTO public.dropdownvalues 
(category, subcategory, itemvalue, displayorder, isactive)
VALUES 
  -- EXPENSE -> VENDOR subcategory
  ('EXPENSE', 'VENDOR', 'Amazon', 1, true),
  ('EXPENSE', 'VENDOR', 'Delta Airlines', 2, true),
  ('EXPENSE', 'VENDOR', 'United Airlines', 3, true),
  ('EXPENSE', 'VENDOR', 'Hilton', 4, true),
  ('EXPENSE', 'VENDOR', 'Marriott', 5, true),
  ('EXPENSE', 'VENDOR', 'Uber', 6, true),
  ('EXPENSE', 'VENDOR', 'Lyft', 7, true),
  ('EXPENSE', 'VENDOR', 'Enterprise', 8, true),
  ('EXPENSE', 'VENDOR', 'Hertz', 9, true),
  ('EXPENSE', 'VENDOR', 'Staples', 10, true),
  ('EXPENSE', 'VENDOR', 'Office Depot', 11, true),
  ('EXPENSE', 'VENDOR', 'Other', 12, true),
  
  -- EXPENSE -> RECEIPT_STATUS subcategory
  ('EXPENSE', 'RECEIPT_STATUS', 'No Receipt', 1, true),
  ('EXPENSE', 'RECEIPT_STATUS', 'Receipt Uploaded', 2, true),
  ('EXPENSE', 'RECEIPT_STATUS', 'Receipt Pending', 3, true),
  ('EXPENSE', 'RECEIPT_STATUS', 'Receipt Missing', 4, true)
ON CONFLICT (category, subcategory, itemvalue) DO NOTHING;

-- Verify the inserts
SELECT category, subcategory, itemvalue, displayorder 
FROM public.dropdownvalues 
WHERE category = 'EXPENSE' 
ORDER BY subcategory, displayorder;
