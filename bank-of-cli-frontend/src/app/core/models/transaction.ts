import { Account } from './account';

/**
 * Transfers produce two records: "transfer-out" on the sender's account
 * and "transfer-in" on the receiver's account.
 */
export type TransactionType = 'deposit' | 'withdraw' | 'transfer-in' | 'transfer-out';

/**
 * One entry in an account's history.
 * `amount` is always positive; use `type` to decide +/- when displaying.
 * See core/contracts/CONTRACTS.md.
 */
export interface Transaction {
  id: string;                     // e.g. "t1"
  accountId: string;              // account this record belongs to
  type: TransactionType;
  amount: number;                 // always > 0
  balanceAfter: number;           // account balance right after this transaction
  date: string;                   // ISO 8601
  description?: string;
  counterpartyAccountId?: string; // only on transfer-in / transfer-out
}

// ---- Request bodies ----

export interface DepositRequest {
  amount: number;
  description?: string;
}

export interface WithdrawRequest {
  amount: number;
  description?: string;
}

export interface TransferRequest {
  destinationAccountId: string;
  amount: number;
  description?: string;
}

// ---- Response body for deposit / withdraw / transfer ----

export interface TransactionResponse {
  id: string;
  accountId: string;
  transactionType: string;
  amount: number;
  newBalance: number;
  createdAt: string;
}

/** True when a transaction adds money to the account. Handy for +/- and colors in the UI. */
export function isCredit(t: Transaction): boolean {
  return t.type === 'deposit' || t.type === 'transfer-in';
}
