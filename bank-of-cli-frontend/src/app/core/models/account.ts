/**
 * A bank account. Each user has one checking and one savings account.
 * `id` is the public account number, so it is also what a user types
 * into the Transfer form as the destination.
 * See core/contracts/CONTRACTS.md.
 */
export interface Account {
  id: string;       // e.g. "ACC-1001"
  userId: string;
  type: 'checking' | 'savings';
  nickname?: string; // optional label the owner sets, max 30 characters
  balance: number;  // dollars, max 2 decimal places
  currency: 'USD';
}

/** PATCH /accounts/{accountId}. Leave a field out to keep it as is; a blank nickname clears it. */
export interface UpdateAccountRequest {
  nickname?: string;
  type?: 'checking' | 'savings';
}

/** POST /accounts. */
export interface CreateAccountRequest {
  type: 'checking' | 'savings';
  nickname?: string;
}