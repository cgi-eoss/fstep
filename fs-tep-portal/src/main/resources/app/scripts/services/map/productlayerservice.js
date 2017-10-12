/**
 * @ngdoc service
 * @name fstepApp.MapService
 * @description
 * # MapService
 * Service in the fstepApp.
 */
define(['../../fstepmodules', 'ol'], function (fstepmodules, ol) {
    'use strict';

    fstepmodules.service('ProductLayerService', ['MapService', '$rootScope', function (MapService, rootScope) {

        var map = MapService.getMap();

        var productLayers = [];

        this.setWMSLayers = function(files) {
            this.clear();
            if(files.length > 0) {
                // Create layer for each output file
                for(var i = 0; i < files.length; i++) {
                    if(files[i]._links.wms){
                        var source = new ol.source.ImageWMS({
                            url: files[i]._links.wms.href,
                            params: {
                                format: 'image/png'
                            },
                            projection: 'EPSG:3857'
                        });
                        var productLayer = new ol.layer.Image({
                            source: source
                        });
                        productLayers.push(productLayer);
                        map.addLayer(productLayer);
                    }
                }

                var metadata = files[files.length-1].metadata;

                if (metadata) {
                    // Zoom into place
                    var polygon = new ol.format.GeoJSON().readGeometry(metadata.geometry, {
                        dataProjection: 'EPSG:4326',
                        featureProjection: 'EPSG:3857'
                    });
                    map.getView().fit(polygon.getExtent(), map.getSize());
                }
            }
        };

        this.clear = function() {
            for(var i = 0; i < productLayers.length; i++){
                map.removeLayer(productLayers[i]);
            }
            productLayers.length = 0;
        };

    }]);
});