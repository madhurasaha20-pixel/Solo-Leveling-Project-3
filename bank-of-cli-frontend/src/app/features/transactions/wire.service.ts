import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { toApiError } from '../../core/services/api-error.util';

export interface WireRequest {
  fromAccountId: string;
  toEmail: string;            // WIRE: was toAccountId
  amount: number;
}

export interface WireResponse {
  transactionId?: string;
  amount?: number;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class WireService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiBase;

  wire(req: WireRequest): Observable<WireResponse> {
    return this.http
      .post<WireResponse>(`${this.api}/wire`, req)
      .pipe(catchError((err) => throwError(() => toApiError(err))));
  }
}