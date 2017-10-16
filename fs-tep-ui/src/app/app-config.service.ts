import {Injectable} from '@angular/core';
import {Http} from '@angular/http';

import 'rxjs/add/operator/toPromise';

@Injectable()
export class AppConfig {

    private config : Promise<any> = null;

    constructor(private http: Http) {
        this.config = new Promise((resolve, reject)=>{
            this.http.get('assets/data/config.json').toPromise().then((response)=>{
                resolve(response.json());
            },
            (error) => {
                reject(error);
            });
        });

    }

    getConfig() {
        return this.config;
    }

};

