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

    constructor(config, timeRange) {

        this.config = config;
        this.timeRange = timeRange;

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

    updateSourceTime(dt: Date) {
        this.source.updateParams({
            TIME: this.getNearestWMSTime(dt)
        });
        //this.source.setTileLoadFunction(this.source.getTileLoadFunction());
        this.source.refresh();
        //this.source.clear();
        //this.source.tileCache.clear()
        //this.source.setTileUrlFunction(this.source.getTileUrlFunction(), function () { return (new Date()).getTime(); });
        if (this.rasterSource) {
            this.rasterSource.changed();
        }
    }

    isTiled() {
        return this.rasterSource == null;
    }

    getLegendUrl() {
        if (this.config.legend) {
            return this.config.url + '?service=WMS&version=1.3.0&request=getLegendGraphic&format=image/png&transparent=true'
                + '&layer=' + this.config.layers
                + '&style=' + (this.config.styles || '')
        }
        else {
            return null;
        }
    }


    getFeatureInfo(coordinates, viewRes) {
        return this.source.getGetFeatureInfoUrl(coordinates, viewRes, 'EPSG:4326', {
            INFO_FORMAT: 'application/json'
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

    getTimeRange() {
        return this.timeRange;
    }

    private getNearestWMSTime(dt: Date): string {

        let end = moment.utc(dt).add(1, 'days');
        let start = moment.utc(dt).subtract(this.timeRange.frequency);

        return start.toISOString() + '/' + end.toISOString();
    
    }

}
