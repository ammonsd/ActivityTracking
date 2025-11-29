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
  ('TASK', 'PROJECT', 'Non-Billable', 2, true),
  
  -- EXPENSE -> EXPENSE_TYPE subcategory (Travel Expenses)
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Airfare', 1, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Hotel', 2, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Ground Transportation', 3, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Rental Car', 4, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Parking', 5, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Mileage', 6, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Meals (Client Meeting)', 7, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Meals (Travel Days)', 8, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Travel - Other', 9, true),
  
  -- EXPENSE -> EXPENSE_TYPE subcategory (Home Office Expenses)
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Internet', 10, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Phone/Mobile', 11, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Office Supplies', 12, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Equipment', 13, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Furniture', 14, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Software/Subscriptions', 15, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Utilities (Portion)', 16, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Home Office - Other', 17, true),
  
  -- EXPENSE -> EXPENSE_TYPE subcategory (Other Business Expenses)
  ('EXPENSE', 'EXPENSE_TYPE', 'Training/Education', 18, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Professional Development', 19, true),
  ('EXPENSE', 'EXPENSE_TYPE', 'Miscellaneous', 20, true),
  
  -- EXPENSE -> PAYMENT_METHOD subcategory
  ('EXPENSE', 'PAYMENT_METHOD', 'Corporate Credit Card', 1, true),
  ('EXPENSE', 'PAYMENT_METHOD', 'Personal Credit Card', 2, true),
  ('EXPENSE', 'PAYMENT_METHOD', 'Cash', 3, true),
  ('EXPENSE', 'PAYMENT_METHOD', 'Check', 4, true),
  ('EXPENSE', 'PAYMENT_METHOD', 'Direct Debit', 5, true),
  ('EXPENSE', 'PAYMENT_METHOD', 'Reimbursement Due', 6, true),
  
  -- EXPENSE -> EXPENSE_STATUS subcategory
  ('EXPENSE', 'EXPENSE_STATUS', 'Draft', 1, true),
  ('EXPENSE', 'EXPENSE_STATUS', 'Submitted', 2, true),
  ('EXPENSE', 'EXPENSE_STATUS', 'Pending Approval', 3, true),
  ('EXPENSE', 'EXPENSE_STATUS', 'Approved', 4, true),
  ('EXPENSE', 'EXPENSE_STATUS', 'Rejected', 5, true),
  ('EXPENSE', 'EXPENSE_STATUS', 'Resubmitted', 6, true),
  ('EXPENSE', 'EXPENSE_STATUS', 'Reimbursed', 7, true),
  
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
  ('EXPENSE', 'RECEIPT_STATUS', 'Receipt Missing', 4, true),
  
  -- EXPENSE -> CLIENT subcategory (shares with TASK)
  ('EXPENSE', 'CLIENT', 'Corporate', 1, true),
  
  -- EXPENSE -> PROJECT subcategory (shares with TASK)
  ('EXPENSE', 'PROJECT', 'General Administration', 1, true),
  ('EXPENSE', 'PROJECT', 'Non-Billable', 2, true)
ON CONFLICT (category, subcategory, itemvalue) DO NOTHING;
