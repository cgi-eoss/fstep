import  {Injectable} from '@angular/core';

import {Http, Response, URLSearchParams} from '@angular/http';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {AppConfig} from '../app-config.service';
import {UserService} from './user.service';

@Injectable()
export class UserPrefsService {
    
    private serviceUrl;

    constructor(private http: Http, private appConfig: AppConfig, private userService: UserService) {
        appConfig.getConfig().then((config)=>{
            this.serviceUrl = config.serviceUrl;
        });
    }

    private extractNameCB = function(type) {
        let regex = new RegExp('^' + type + '\\.');

        return function(record) {
            record.name = record.name.replace(regex, '');
        };
    };

    getPreferences = function(type) {

        let params = new URLSearchParams();
        params.set('type', type);
        params.set('owner', this.userService.getCurrentUser().href);
        return this.http.get(this.serviceUrl + '/userPreferences/search/search', { 
            search: params 
        })
        .toPromise()
        .then((response)=>{
            var replaceCb = this.extractNameCB(type);

            let values = response.json()._embedded.userPreferences.map(function(element){
                replaceCb(element);
                return element;
            });

            return values
        });


    };

    getPreference = function(id) {

        return this.http.get(this.serviceUrl + '/userPreferences/' + id)
        .toPromise()
        .then((response)=>{

            let data = response.json();
            this.extractNameCB(data.type)(data);
            return data;
        });
    };

    getPreferenceByName = function(type, name) {

        let params = new URLSearchParams();
        params.set('name', type + '.' + name);
        params.set('owner', this.userService.getCurrentUser().href);
        return this.http.get(this.serviceUrl + '/userPreferences/search/search', { 
            search: params 
        })
        .toPromise()
        .then((response)=>{

            let value = response.json()._embedded.userPreferences[0];
            this.extractNameCB(type)(value);
            return value;
        });
    };


}