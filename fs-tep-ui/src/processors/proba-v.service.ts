import  {Injectable} from '@angular/core';
import {Http, Response, URLSearchParams} from '@angular/http';

import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/map';

@Injectable()
export class ProbaVService {

    private serviceUrl = '/proba-v/';

    constructor(private http: Http) {

    }

    getTimeSeries(config) {
        
        let params = new URLSearchParams();
        params.set('startDate', config.startDate);
        params.set('endDate', config.endDate);
        params.set('lat', config.lat);
        params.set('lon', config.lon);

        let url = this.serviceUrl + 'timeseries/v1.0/ts/' + config.productId + '/point';

        return this.http.get(url, { 
            search: params 
        })
        .map(this.parseResponse_)
        .catch(this.handleError_);

    }

    private parseResponse_(res: Response) {
        return res.json();
    }

    private handleError_(error: Response | any) {
        let errMsg: string;
        if (error instanceof Response) {
            const body = error.json() || '';
            const err = body.error || JSON.stringify(body);
            errMsg = `${error.status} - ${error.statusText || ''} ${err}`;
        }
        else {
            errMsg = error.message ? error.message : error.toString();
        }

        return Observable.throw(errMsg);
    }

};
