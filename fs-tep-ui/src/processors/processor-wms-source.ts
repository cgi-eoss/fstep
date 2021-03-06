import TileWMS from 'ol/source/tilewms';
import RasterSource from 'ol/source/raster';

import {ColorMap} from './colormap';
import * as moment from 'moment';

export class ProcessorWMSSource {
    
    private config;
    
    private source;
    private rasterSource;

    private colorMap;

    private timeRange: {start: moment.Moment, end: moment.Moment, frequency: moment.Duration}
    private domainConfig;
    private legendConfig;
    private infoFormat;
    private extent;
    private options;

    constructor(config, timeRange, domainConfig, legendConfig, infoFormat, extent, options) {

        this.config = config;
        this.timeRange = timeRange;
        this.domainConfig = domainConfig;
        this.legendConfig = legendConfig || {};
        this.infoFormat = infoFormat || 'application/json';
        this.extent = extent;
        this.options = options || {};

        let source =  new TileWMS({
            url: config.url,
            params: Object.assign({}, {
                LAYERS: config.layers,
                STYLES: config.styles || ''
            }, config.parameters),
            projection: config.srs || 'EPSG:4326'
        });

        if (config.colormap) {

            let colormap = new ColorMap({
                steps: config.colormap
            });

            source.updateParams({
                STYLES: 'fs-prods:grayscale'
            });

            let raster_source = new RasterSource({
                sources: [source],
                //threads: 0,
                operation: function(pixels, data) {
                    let pixel = pixels[0];
                    let p = data.colormap[pixel[0]].slice();
                    p[3] = ~~(p[3] * (pixel[3] / 255));

                    return p;
                }
            });

            raster_source.on('beforeoperations', (event) => {
                event.data.colormap = colormap.getForwardArray();
            });

            
            this.rasterSource = raster_source;
            this.colorMap = colormap;
        }

        this.source = source;

    }

    getOLSource() {
        return this.rasterSource ? this.rasterSource : this.source;
    }

    getOptions() {
        return this.options;
    }

    updateSourceTime(dt: Date) {

        if (!this.timeRange) {
            return;
        }

        this.source.updateParams({
            TIME: this.getNearestWMSTime(dt)
        });
        
        this.source.refresh();
        
        if (this.rasterSource) {
            this.rasterSource.changed();
        }
    }

    isTiled() {
        return this.rasterSource == null;
    }

    hasTimeDimension() {
        return !!this.timeRange;
    }

    getExtent() {
        return this.extent;
    }

    getLegendUrl() {
        if (this.config.legend) {
            return this.config.url + '?service=WMS&version=1.3.0&request=getLegendGraphic&format=image/png&transparent=true'
                + '&layer=' + this.config.layers
                + '&style=' + (this.config.styles || '')
                + '&legend_options=layout:horizontal;fontColor:ffffff;dx:5;fontAntiAliasing:true'
                + '&width=15&height=' + (this.legendConfig.height || 10)
        }
        else {
            return null;
        }
    }


    getFeatureInfo(coordinates, viewRes, viewProj) {
        return this.source.getGetFeatureInfoUrl(coordinates, viewRes, viewProj || 'EPSG:4326', {
            INFO_FORMAT: this.infoFormat
        })
    }

    getTimeSeries(coordinates, viewRes) {

    }

    getColorMap() {
        return this.colorMap;
    }

    getDomain() {
        return this.config.domain;
    }

    scaleValue(value, forceNumeric?) {
        let domain = this.domainConfig;
        if (domain) {
            if (value === domain.nodata) {
                return "No data";
            } else if (domain.special_values && !forceNumeric) {
                let sv = domain.special_values[value];
                if (sv) {
                    return sv;
                }
            }
            return domain.offset  + (value * domain.scale); 
        }

        return value;
    }

    getTimeRange() {
        return this.timeRange;
    }

    private getNearestWMSTime(dt: Date): string {

        let end = moment.utc(dt).add(1, 'days');
        let start = moment.utc(dt).subtract(this.timeRange.frequency);

        return start.toISOString() + '/' + end.toISOString();
    
    }

}
