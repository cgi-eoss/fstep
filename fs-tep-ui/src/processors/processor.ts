import {ProcessorWMSSource} from './processor-wms-source';
import * as moment from 'moment';

export class Processor {
    id: string;
    name: string;
    description: string;
    thumb: string;
    mapSource;
    timeRange: {start: moment.Moment, end: moment.Moment, frequency: moment.Duration, list: Array<moment.Moment>}

    constructor(config) {
        this.id = config.id;
        this.name = config.name;
        this.description = config.description;
        this.thumb = config.thumb;
        this.timeRange = {
            start: moment(config.time_range.start),
            end: moment(config.time_range.end),
            frequency: config.time_range.frequency ? moment.duration(config.time_range.frequency) : null,
            list: config.time_range.list ? config.time_range.list.map((ts)=>{
                return moment(ts);
            }) : null
        }
        this.mapSource = this.createMapSource(config.layer);
    }


    private createMapSource(params) {
        if (params.type == 'WMS') {
            return new ProcessorWMSSource(params.config, this.timeRange);
        }
    }
}