export interface Expense {
  id?: number;
  expenseDate: string;
  client: string;
  project?: string;
  expenseType: string;
  description: string;
  amount: number;
  currency: string;
  paymentMethod: string;
  vendor?: string;
  referenceNumber?: string;
  receiptPath?: string;
  receiptStatus?: string;
  expenseStatus: string;
  approvedBy?: string;
  approvalDate?: string;
  approvalNotes?: string;
  reimbursedAmount?: number;
  reimbursementDate?: string;
  reimbursementNotes?: string;
  notes?: string;
  username?: string;
}
