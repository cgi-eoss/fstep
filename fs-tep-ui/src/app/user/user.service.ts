import  {Injectable} from '@angular/core';

import {Http, Response, URLSearchParams} from '@angular/http';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {UserProfile} from './user-profile';

@Injectable()
export class UserService {
    private user : BehaviorSubject<UserProfile> = new BehaviorSubject(null);

    constructor(private http: Http) {

    }

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

    getUser() {
        return this.user.asObservable();
    }

    getCurrentUser() {
        return this.user.value;
    }

}