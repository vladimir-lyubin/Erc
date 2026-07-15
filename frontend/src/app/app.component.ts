import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <span class="brand">Exchange Rate Management</span>
      <nav>
        <a routerLink="/calculator" routerLinkActive="active">Calculator</a>
        <a routerLink="/historical" routerLinkActive="active">Historical &amp; Trend</a>
        <a routerLink="/analytics" routerLinkActive="active">Analytics</a>
      </nav>
    </header>
    <main class="content">
      <router-outlet />
    </main>
  `,
})
export class AppComponent {}
