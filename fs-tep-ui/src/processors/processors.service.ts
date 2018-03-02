import {Injectable} from '@angular/core';
import {Http, Response, URLSearchParams} from '@angular/http';

import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import 'rxjs/add/operator/toPromise';

import WMSCapabilities from 'ol/format/wmscapabilities';

import {TimeService} from '../app/time.service';
import {BreadcrumbService} from '../common/breadcrumb.component/breadcrumb.service';
import {AppConfig} from '../app/app-config.service';

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
        end: new Date,
        units: '',
        title: ''
    });
    
    constructor(private http: Http, private timeService: TimeService, breadcrumbService: BreadcrumbService, private appConfig: AppConfig) {
        this.timeSeriesState.value.start.setMonth(this.timeSeriesState.value.start.getMonth() - 6);

        
        breadcrumbService.addFriendlyNameForRoute('/processor', 'Products');
        breadcrumbService.addCallbackForRouteRegex('^/processor/.*$', this.getProcessorName.bind(this));
    }


    getProcessorsList() : Promise<Array<Processor>> {
        return new Promise((resolve, reject)=>{
            if (this.processors) {
                resolve(this.processors);
            }
            else {
                this.appConfig.getConfig().then((config) => {
                    
                    this.http.get(config.geoserver.url + '/' + config.geoserver.products_workspace + '/ows', {
                        params: {
                            request: 'getCapabilities',
                            version: '1.3.0',
                            service: 'WMS'
                        }
                    })
                    .toPromise().then((response)=>{
                        let parser = new WMSCapabilities();
                        let capabilities = parser.read(response.text());
                        let layers = capabilities.Capability.Layer.Layer;

                        console.log(layers);
                        this.processors  = layers.map((layer) => {

                            let timeDimension = layer.Dimension.find((dim)=>{
                                return dim.name == 'time'
                            });

                            if (!timeDimension) {
                                return;
                            }

                            if (!timeDimension.values || !/,/.test(timeDimension.values)) {
                                return;
                            }

                            let times = timeDimension.values.split(',');

                            let additionalConfig = config.products[layer.Name] || {}

                            let processor = null;

                            try {
                                processor =  new Processor({
                                    id: layer.Name.toLowerCase(),
                                    name: layer.Title,
                                    description: layer.Abstract,
                                    layer: {
                                        type: "WMS",
                                        config: {
                                            url: config.geoserver.url + '/'  + config.geoserver.products_workspace + '/wms',
                                            layers: layer.Name,
                                            legend: true,
                                            srs: 'EPSG:900913'
                                        }
                                    },
                                    time_range: {
                                        start: times[0],
                                        end: times[times.length - 1],
                                        list: times
                                    },
                                    ...additionalConfig
                                });
                            }
                            catch(e) {

                            }

                            return processor;

                        }).filter((processor) => {
                            return processor != null;
                        });
                        
                        resolve(this.processors);
                    });
                });
            }
        })
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

            this.timeService.setSelectedDate(processor.getNearestTime(dt));

            let tsState = this.timeSeriesState.value;
            if (processor.timeRange.end.isBefore(tsState.end)) {
                tsState.end = processor.timeRange.end.toDate();
            }
            if (processor.timeRange.start.isAfter(tsState.start)) {
                tsState.start = processor.timeRange.start.toDate();
            }

            tsState.units = processor.domainConfig.units;
            tsState.title = processor.domainConfig.title || processor.name;
            
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