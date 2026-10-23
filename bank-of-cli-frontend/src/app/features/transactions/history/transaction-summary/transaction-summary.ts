import { CurrencyPipe } from '@angular/common';
import { Component, input } from '@angular/core';

@Component({
  selector: 'transaction-summary',
  imports: [CurrencyPipe],
  templateUrl: './transaction-summary.html',
  // styleUrls: ['./transaction-history.component.css']
})
export class TransactionSummary {
  readonly numberOfTransactions = input<number>(0);
  readonly totalIn = input<number>(0);
  readonly totalOut = input<number>(0);
}