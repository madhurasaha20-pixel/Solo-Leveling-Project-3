import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guards';
import { AuthPageComponent } from './features/auth/auth-page.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { TransactionsComponent } from './features/transactions/transactions.component';
import { TransactionHistoryComponent } from './features/transactions/history/transaction-history.component';
import { ProfileComponent } from './features/profile/profile.component';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    component: DashboardComponent,
    title: 'Dashboard · Bank of CLI',
    data: {
      title: 'Dashboard',
      description: 'Here is where your accounts stand today.',
      greeting: true
    }
  },
  {
    path: 'move-money',
    canActivate: [authGuard],
    component: TransactionsComponent,
    title: 'Manage Money · Bank of CLI'
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    component: AuthPageComponent,
    title: 'Sign in · Bank of CLI'
  },
  {
    path: 'transactions',
    canActivate: [authGuard],
    component: TransactionHistoryComponent,
    title: 'Transactions · Bank of CLI'
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    component: ProfileComponent,
    title: 'Profile · Bank of CLI'
  },
  { path: '**', redirectTo: 'login' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
