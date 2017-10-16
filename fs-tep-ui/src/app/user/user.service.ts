import  {Injectable} from '@angular/core';

import {Http, Response, URLSearchParams} from '@angular/http';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {AppConfig} from '../app-config.service';

import {UserProfile} from './user-profile';

@Injectable()
export class UserService {
    private user : BehaviorSubject<UserProfile> = new BehaviorSubject(null);
    private serviceUrl;

    constructor(private http: Http, private appConfig: AppConfig) {
        appConfig.getConfig().then((config)=>{
            this.serviceUrl = config.serviceUrl;
            this.getLoggedUser();
        });
    }

    /*
    login(username: string) {
        return this.http.get('assets/data/users/' + username + '.json')
            .toPromise()
            .then((response)=>{
                let user_data = response.json();
                this.user.next({
                    name: user_data.name,
                    subscription: {
                        area: user_data.subscription.area
                    }
                });
            })
            .catch((error)=>{
                this.user.next(null);
            });
    }
    */

    private getLoggedUser() {
        return this.http.get(this.serviceUrl + '/users/current')
            .toPromise()
            .then((response)=>{
                let userData = response.json();
                this.user.next({
                    name: userData.name,
                    subscription: {
                        area: [[[[-180, -90], [-180, 90], [180, 90], [180, -90], [-180, -90]]]]
                    },
                    href: userData._links.self.href
                });
            })
       
    }

    getUser() {
        return this.user.asObservable();
    }

    getCurrentUser() {
        return this.user.value;
    }

}