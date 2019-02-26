import {ProcessorWMSSource} from './processor-wms-source';
import * as moment from 'moment';

export class Processor {
    id: string;
    name: string;
    description: string;
    thumb: string;
    mapSource;
    domainConfig;
    legendConfig;
    infoFormat;
    timeRange?: {start: moment.Moment, end: moment.Moment, frequency: moment.Duration, list: Array<moment.Moment>}

    constructor(config) {
        this.id = config.id;
        this.name = config.name;
        this.description = config.description;
        this.thumb = config.thumb;
        if (config.time_range) {
            this.timeRange = {
                start: moment(config.time_range.start),
                end: moment(config.time_range.end),
                frequency: config.time_range.frequency ? moment.duration(config.time_range.frequency) : null,
                list: config.time_range.list ? config.time_range.list.map((ts)=>{
                    return moment(ts);
                }) : null
            }
        }
        this.domainConfig = config.domain;
        this.legendConfig = config.legend;
        this.infoFormat = config.infoFormat;
        this.mapSource = this.createMapSource(config.layer);
    }

    hasTimeDimension() {
        return !!this.timeRange;
    }

    getNearestTime(dt) {
        if (this.timeRange.end.isBefore(dt)) {
            return this.timeRange.end.toDate();
        }
        else if (this.timeRange.start.isAfter(dt)) {
            return this.timeRange.start.toDate();
        }
        else {
            let times = this.timeRange.list;

            let nearestIdx = 0;
            while (dt.getTime() >= times[nearestIdx + 1]) {
                nearestIdx++;
            }
            return times[nearestIdx].toDate();
        }
    }


    private createMapSource(params) {
        if (params.type == 'WMS') {
            return new ProcessorWMSSource(params.config, this.timeRange, this.domainConfig, this.legendConfig, this.infoFormat);
        }
    }
}