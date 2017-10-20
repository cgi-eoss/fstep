import {Component, ViewChild, ElementRef, AfterViewInit} from "@angular/core";
import {Router, ActivatedRoute, NavigationEnd} from '@angular/router';

import 'rxjs/add/operator/map';

import {UserService} from '../user/user.service';


@Component({
  selector: 'nav-bar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements AfterViewInit {

  username: string;
  public menuVisible = false;
  public activeTab = '';

  constructor(private userService: UserService, private router: Router, private route: ActivatedRoute) {
    
    this.userService.getUser().subscribe((user)=> {
      this.username = user ? user.name : null;
    })

    this.router.events.subscribe((navigationEnd: NavigationEnd)=>{
      this.menuVisible = false;
      this.activeTab = '';
      if (navigationEnd.url.search(/\/processor/) != -1) {
        this.activeTab = 'products';
      }
    });
    
  }

  ngAfterViewInit() {
    
  }

  onMobileMenuClick() {
    this.menuVisible = !this.menuVisible;
  }
}