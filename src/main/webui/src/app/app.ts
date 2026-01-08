import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, MatIconModule],
  templateUrl: './app.html',
  styleUrl: './app.less'
})
export class App {
  protected title = 'passport';
}
