import {Injectable} from '@angular/core';
import {Http, Response, URLSearchParams} from '@angular/http';

import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import 'rxjs/add/operator/toPromise';

import {TimeService} from '../app/time.service';
import {BreadcrumbService} from '../common/breadcrumb.component/breadcrumb.service';

import {Processor} from './processor';

@Injectable()
export class ProcessorsService {

    private serviceUrl = 'assets/data/';
    private processors: Array<Processor> = null;
    private activeProcessor: BehaviorSubject<Processor> = new BehaviorSubject(null);
    private timeSeriesState = new BehaviorSubject({
        enabled: false,
        coordinates: [0, 0],
        start: new Date(), 
        end: new Date
    });
    
    constructor(private http: Http, private timeService: TimeService, breadcrumbService: BreadcrumbService) {
        this.timeSeriesState.value.start.setMonth(this.timeSeriesState.value.start.getMonth() - 6);

        
        breadcrumbService.addFriendlyNameForRoute('/processor', 'Products');
        breadcrumbService.addCallbackForRouteRegex('^/processor/.*$', this.getProcessorName.bind(this));
    }

    getProcessorsList() : Promise<Array<Processor>> {
        if (!this.processors) {
            return this.http.get(this.serviceUrl + 'processors.json')
                .toPromise()
                .then((response)=>{
                    this.processors = response.json().map((el)=>{
                        return new Processor(el);
                    });
                    return this.processors
                })
                .catch((error)=>{
                    return Promise.reject(error.message || error);
                });
            }
        else {
            return new Promise((resolve, reject)=>{
                resolve(this.processors);
            });
        }
    }

    getProcessor(id): Promise<Processor>{
        return this.getProcessorsList().then((processors)=>{
            return processors.find((el)=>{
                return el.id == id;
            })
        })
    }

    getProcessorName(id) {
        let processor =  this.processors ? this.processors.find((el)=>{
            return el.id == id;
        }) : null;
        return processor ? processor.name : id;
    }

    
    setActiveProcessor(processor) {
        this.activeProcessor.next(processor);
        
        this.timeService.setDateContraints(null);

        if (processor) {

            this.timeService.setDateContraints({
                minDate: processor.timeRange.start ? processor.timeRange.start.toDate(): null,
                maxDate: processor.timeRange.end ? processor.timeRange.end.toDate(): null,
                dateList: processor.timeRange.list ? processor.timeRange.list.map((el)=>el.toDate()) : null
            });

            let dt = this.timeService.getCurrentDate();
            if (processor.timeRange.end.isBefore(dt)) {
                this.timeService.setSelectedDate(processor.timeRange.end.toDate());
            }
            else if (processor.timeRange.start.isAfter(dt)) {
                this.timeService.setSelectedDate(processor.timeRange.start.toDate());
            }



            let outOfRange = false;
            let tsState = this.timeSeriesState.value;
            if (processor.timeRange.end.isBefore(tsState.end)) {
                tsState.end = processor.timeRange.end.toDate();
                outOfRange = true;
            }
            if (processor.timeRange.start.isAfter(tsState.start)) {
                tsState.start = processor.timeRange.start.toDate();
                outOfRange = true;
            }
            if (outOfRange)
                this.setTimeSeriesState(tsState);
        }
    }

    getActiveProcessor() {
        return this.activeProcessor.value;
    }

    getActiveProcessorObs() {
        return this.activeProcessor.asObservable();
    }

    getTimeSeriesState() {
        return this.timeSeriesState.asObservable();
    }

    getTimeSeriesStateValue() {
        return this.timeSeriesState.value;
    }

    setTimeSeriesState(state) {
        this.timeSeriesState.next(Object.assign({}, this.timeSeriesState.value, state))
    }

}