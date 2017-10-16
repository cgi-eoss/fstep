import {Component, ViewChild, ElementRef, AfterViewInit} from "@angular/core";
import {Router, ActivatedRoute} from '@angular/router';

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

  constructor(private userService: UserService, private router: Router, private route: ActivatedRoute) {
    
    this.userService.getUser().subscribe((user)=> {
      this.username = user ? user.name : null;
    })

    this.router.events.subscribe((path)=>{
      this.menuVisible = false;
    });
    

    //this.userService.login('admin');
  }

  ngAfterViewInit() {
    
    /*
    let user = this.route
      .queryParamMap
      .map(params => params.get('user'));

    user.subscribe((val)=>{
        if (val)
          this.userSerice.login(val);
    });
    */
    
    /*
    let user = this.route.snapshot.queryParamMap.get('user') || 'aaa';
    this.userSerice.login(user);
    */
  }

  onMobileMenuClick() {
    this.menuVisible = !this.menuVisible;
  }
}