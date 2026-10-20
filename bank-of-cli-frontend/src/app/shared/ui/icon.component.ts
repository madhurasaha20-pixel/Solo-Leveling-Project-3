import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type IconName = 'check' | 'check-circle' | 'x' | 'info' | 'alert' | 'chevron-down' | 'copy' | 'sun' | 'moon' | 'log-out' | 'trash';

const PATHS: Record<IconName, string> = {
  check: 'M4.5 10.5l3.5 3.5 7.5-8',
  'check-circle': 'M7 10.2l2 2 4-4.4',
  x: 'M5 5l10 10M15 5L5 15',
  info: 'M10 9v5M10 6.5v.01',
  alert: 'M10 6v5M10 13.5v.01',
  'chevron-down': 'M6 8l4 4 4-4',
  copy: 'M8 7.5h7.5v9H8zM12 7.5V3.5H4.5v9H8',
  sun: 'M10 7a3 3 0 1 0 0 6a3 3 0 1 0 0-6zM10 2.5V4M10 16v1.5M2.5 10H4M16 10h1.5M4.7 4.7l1.06 1.06M14.24 14.24l1.06 1.06M4.7 15.3l1.06-1.06M14.24 5.76l1.06-1.06',
  moon: 'M16.5 11.5A6.5 6.5 0 0 1 8.5 3.5a6.5 6.5 0 1 0 8 8z',
  'log-out': 'M8 3.5H4.5v13H8M13 13.5l3.5-3.5L13 6.5M16.5 10H8',
  trash: 'M3.5 5.5h13M8 5.5v-2h4v2M5 5.5l.75 11h8.5l.75-11M8.5 9v4.5M11.5 9v4.5',
};
const CIRCLED: IconName[] = ['check-circle', 'info', 'alert'];

/**
 * 20px line icons, 1.75 stroke, currentColor — same geometry family as Lucide.
 * If you add lucide-angular later, keep size 20 and strokeWidth 1.75.
 */
@Component({
  selector: 'bc-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'inline-flex shrink-0', 'aria-hidden': 'true' },
  template: `
    <svg [attr.width]="size()" [attr.height]="size()" viewBox="0 0 20 20" fill="none" stroke="currentColor"
         stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round">
      @if (circled()) { <circle cx="10" cy="10" r="7.25" /> }
      <path [attr.d]="path()" />
    </svg>
  `,
})
export class IconComponent {
  readonly name = input.required<IconName>();
  readonly size = input(20);
  protected readonly path = computed(() => PATHS[this.name()]);
  protected readonly circled = computed(() => CIRCLED.includes(this.name()));
}
