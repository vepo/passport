import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { ActivatedRoute, Router, RouterLink, RouterOutlet } from '@angular/router';
import { RoleDirective } from './directives/role.directive';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, MatIconModule, MatMenuModule, RoleDirective],
  templateUrl: './app.html',
  styleUrl: './app.less'
})
export class App {
  protected title = 'passport';
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  isAuthenticated(): boolean {
    return this.authService.isLoggedIn();
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
