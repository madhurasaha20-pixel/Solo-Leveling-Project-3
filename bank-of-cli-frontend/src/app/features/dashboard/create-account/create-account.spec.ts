import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { AccountService } from '../../../core/services/account.service';
import { CreateAccountComponent } from './create-account';

describe('CreateAccountComponent', () => {
  let component: CreateAccountComponent;
  let fixture: ComponentFixture<CreateAccountComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateAccountComponent],
      providers: [
        provideNoopAnimations(),
        {
          provide: AccountService,
          useValue: {
            createAccount: () => of({
              id: 'ACC-123',
              userId: 'user-123',
              type: 'checking',
              nickname: undefined,
              balance: 0,
              currency: 'USD',
            }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateAccountComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});