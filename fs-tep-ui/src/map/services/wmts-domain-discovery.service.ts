import { Injectable } from '@angular/core';
import { Http } from '@angular/http';

@Injectable()
export class WmtsDomainDiscoveryService {

    private domParser = new DOMParser();
    private xmlNamespaces = {
        ows: 'http://www.opengis.net/ows/1.1',
        wmts_md: 'http://demo.geo-solutions.it/share/wmts-multidim/wmts_multi_dimensional.xsd',
        wmts: 'http://www.opengis.net/wmts/1.0'
    };

    constructor(private http: Http) {

    }

    describeDomains(parameters: {
        url: string,
        layer: string,
        tileMatrix: string,
        bbox?: string,
        version?: string,
        restrictions?: Array<{dimension: string, range: string}>
    }) {

        let params: any = {
            service: 'WMTS',
            version: parameters.version || '1.0.0',
            request: 'DescribeDomains',
            layer: parameters.layer,
            tileMatrix: parameters.tileMatrix
        };

        if (parameters.bbox) {
            params.BBOX = parameters.bbox;
        }

        parameters.restrictions = parameters.restrictions || [];

        params = parameters.restrictions.reduce((options, item) => {
            return {
                [item.dimension]: item.range,
                ...options
            };
        }, params);

        return this.http.get(parameters.url, {params: params}).toPromise().then((response) => {
            return this.parseDescribeDomainsResponse(response.text());
        });
    }


    getHistogram(parameters: {
        url: string,
        layer: string,
        tileMatrix: string,
        dimension: string,
        resolution?: string,
        bbox?: string,
        restrictions?: Array<{dimension: string, range: string}>,
        version?: string
    }) {

        let params: any = {
            service: 'WMTS',
            version: parameters.version || '1.0.0',
            request: 'GetHistogram',
            layer: parameters.layer,
            histogram: parameters.dimension,
            tileMatrix: parameters.tileMatrix,
            resolution: parameters.resolution
        };

        if (parameters.bbox) {
            params.BBOX = parameters.bbox;
        }

        parameters.restrictions = parameters.restrictions || [];

        params = parameters.restrictions.reduce((options, item) => {
            return {
                [item.dimension]: item.range,
                ...options
            };
        }, params);

        return this.http.get(parameters.url, {params: params}).toPromise().then((response) => {
            return this.parseGetHistogramResponse(response.text());
        });
    }

    getFeature(parameters: {
        url: string,
        layer: string,
        tileMatrix: string,
        bbox?: string,
        restrictions?: Array<{dimension: string, range: string}>,
        version?: string
    }) {

        let params: any = {
            service: 'WMTS',
            version: parameters.version || '1.0.0',
            request: 'GetFeature',
            layer: parameters.layer,
            tileMatrix: parameters.tileMatrix
        };

        if (parameters.bbox) {
            params.BBOX = parameters.bbox;
        }

        parameters.restrictions = parameters.restrictions || [];

        params = parameters.restrictions.reduce((options, item) => {
            return {
                [item.dimension]: item.range,
                ...options
            };
        }, params);

        return this.http.get(parameters.url, {params: params}).toPromise().then((response) => {
            return this.parseGetFeatureResponse(response.text());
        });
    }


    private parseDescribeDomainsResponse(response) {

        try {
            let doc = this.domParser.parseFromString(response, 'application/xml');
            let domains = doc.getElementsByTagNameNS(this.xmlNamespaces.wmts_md, 'Domains')[0];

            let space = domains.getElementsByTagNameNS(this.xmlNamespaces.wmts_md, 'SpaceDomain')[0];
            let bbox = space.getElementsByTagNameNS(this.xmlNamespaces.wmts_md, 'BoundingBox')[0];

            let dimensions = domains.getElementsByTagNameNS(this.xmlNamespaces.wmts_md, 'DimensionDomain');

            return {
                bbox: {
                    minx: parseFloat(bbox.getAttribute('minx')),
                    maxx: parseFloat(bbox.getAttribute('maxx')),
                    miny: parseFloat(bbox.getAttribute('miny')),
                    maxy: parseFloat(bbox.getAttribute('maxy'))
                },
                domains: Array.from(dimensions).map((item) => {
                    return {
                        dimension: item.getElementsByTagNameNS(this.xmlNamespaces.ows, 'Identifier')[0].childNodes[0].nodeValue,
                        range: item.getElementsByTagNameNS(this.xmlNamespaces.wmts_md, 'Domain')[0].childNodes[0].nodeValue,
                        size: parseInt(item.getElementsByTagNameNS(this.xmlNamespaces.wmts_md, 'Size')[0].childNodes[0].nodeValue)
                    };
                })
            };

        } catch (e) {
            return null;
        }

    }

    private parseGetHistogramResponse(response) {
        let doc = this.domParser.parseFromString(response, 'application/xml');
    }

    private parseGetFeatureResponse(response) {
        let doc = this.domParser.parseFromString(response, 'application/xml');
        let collection = doc.getElementsByTagNameNS(this.xmlNamespaces.wmts, 'FeatureCollection')[0];
        let features = collection.getElementsByTagNameNS(this.xmlNamespaces.wmts, 'feature');

        return Array.from(features).map((item) => {
            let dimensions_el = item.getElementsByTagNameNS(this.xmlNamespaces.wmts, 'dimension');
            let dimensions = Array.from(dimensions_el).map((dimension) => {
                return {
                    name: dimension.getAttribute('name'),
                    value: dimension.childNodes[0].nodeValue,
                };
            });

            return {
                dimensions: dimensions,
                footprint: []
            };
        });
    }

}
