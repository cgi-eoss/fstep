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

    private serviceNames = {};
    private processors: Map<string, Array<Processor>> = new Map();
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

        
        breadcrumbService.addFriendlyNameForRoute('/products', 'Products');
        breadcrumbService.addCallbackForRouteRegex('^^/products/([^/]*)/?$', (matches) => {
            return this.getServiceName(matches[1]);
        });
        breadcrumbService.addCallbackForRouteRegex('^/products/([^/]*)/([^/]*)/?$', (matches) => {
            return this.getProcessorName(matches[1], matches[2]);
        });

        this.appConfig.getConfig().then((config) => {
            config.categories.forEach((service) => {
                this.serviceNames[service.id] = service;
            });
        });
    }

    getServiceList() {
        return this.appConfig.getConfig().then((config) => {
            return config.categories;
        });
    }

    getProcessorsList(service: string) : Promise<Array<Processor>> {
        return new Promise((resolve, reject)=>{
            if (this.processors.get(service)) {
                resolve(this.processors.get(service));
            }
            else {
                this.appConfig.getConfig().then((config) => {

                    let geoserverUrl = this.serviceNames[service].url;
                    
                    this.http.get(geoserverUrl + '/ows', {
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

                        this.processors.set(service, this.serviceNames[service].products.map((product) => {

                            let layer = layers.find((layer) => {
                                return layer.Name === product.layer
                            });

                            if (!layer) {
                                return null;
                            }
                            
                            let times = null;

                            if (layer.Dimension) {
                                let timeDimension = layer.Dimension.find((dim)=>{
                                    return dim.name == 'time'
                                });

                                if (timeDimension && timeDimension.values && /,/.test(timeDimension.values) ) {
                                    times = timeDimension.values.split(',');
                                }
                            }

                            
                            let processor = null;

                            try {

                                let processorConfig = {
                                    ...product,
                                    name: layer.Title,
                                    description: layer.Abstract,
                                    layer: {
                                        type: "WMS",
                                        config: {
                                            url: geoserverUrl + '/wms',
                                            layers: layer.Name,
                                            legend: true,
                                            srs: 'EPSG:900913'
                                        }
                                    }
                                }

                                if (times) {
                                    processorConfig.time_range = {
                                        start: times[0],
                                        end: times[times.length - 1],
                                        list: times
                                    }
                                }

                                processor =  new Processor(processorConfig);
                            }
                            catch(e) {

                            }

                            return processor;

                        }).filter((processor) => {
                            return processor != null;
                        }));
                        
                        resolve(this.processors.get(service));
                    });
                });
            }
        })
    }

    getProcessor(service, id): Promise<Processor>{
        return this.getProcessorsList(service).then((processors)=>{
            return processors.find((el)=>{
                return el.id == id;
            })
        })
    }

    getProcessorName(serviceId, productId) {
        let product = null;
        let service = this.processors.get(serviceId);
        if (service) {
            product = service.find((el)=>{
                return el.id == productId;
            });
        }

        return product ? product.name : productId;
    }

    getServiceName(service) {
        return this.serviceNames[service] ? this.serviceNames[service].name : service;
    }
    
    setActiveProcessor(processor) {
        this.activeProcessor.next(processor);
        
        this.timeService.setDateContraints(null);

        if (processor) {

            if (processor.hasTimeDimension()) {

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

            } else {
                let tsState = this.timeSeriesState.value;
                tsState.enabled = false;
                this.setTimeSeriesState(tsState);
            }


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