import {Component, ViewChild, ElementRef, AfterViewInit} from "@angular/core";
import {Router, ActivatedRoute, NavigationEnd} from '@angular/router';

import 'rxjs/add/operator/map';

import {UserService} from '../user/user.service';
import {AppConfig} from '../app-config.service';

@Component({
  selector: 'nav-bar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements AfterViewInit {

  username: string;
  public menuVisible = false;
  public services = [];
  public activeTab = '';

  constructor(
    private userService: UserService, 
    private router: Router, 
    private route: ActivatedRoute,
    private appConfig: AppConfig
  ) {
    
    this.userService.getUser().subscribe((user) => {
      this.username = user ? user.name : null;
    })

    this.router.events.subscribe((navigationEnd: NavigationEnd)=>{
      this.menuVisible = false;
      this.activeTab = '';
      let matches = navigationEnd.url.match(/\/products\/([^\/]*)/);
      if (matches) {
        this.activeTab = matches[1];
      } else {
        this.activeTab = '';
      }
    });
    
  }

  ngAfterViewInit() {

    this.appConfig.getConfig().then((config)=>{
      this.services = config.categories;
    });
    
  }

  onMobileMenuClick() {
    this.menuVisible = !this.menuVisible;
  }
}