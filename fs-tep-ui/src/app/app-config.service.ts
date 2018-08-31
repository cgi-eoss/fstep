import {Injectable} from '@angular/core';
import {Http} from '@angular/http';

import 'rxjs/add/operator/toPromise';

@Injectable()
export class AppConfig {

    private config : Promise<any> = null;

    constructor(private http: Http) {
        this.config = new Promise((resolve, reject)=>{

            let configUrls = [
                'assets/data/config.json',
                '../config/analyst.json'
            ]

            Promise.all(configUrls.map((url) => {
                return this.http.get(url).toPromise().then((response)=>{
                    return response.json();
                }).catch((err) => {
                    return null;
                })
            })).then((configs) => {
                if (!configs[0] && !configs[1]) {
                    reject();
                }
                resolve({
                    ...(configs[0] || {}),
                    ...(configs[1] || {})
                });
            });
        });

    }

    getConfig() {
        return this.config;
    }

};

