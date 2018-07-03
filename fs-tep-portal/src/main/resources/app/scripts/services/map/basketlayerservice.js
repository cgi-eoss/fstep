/**
 * @ngdoc service
 * @name fstepApp.MapService
 * @description
 * # MapService
 * Service in the fstepApp.
 */
define(['../../fstepmodules', 'ol'], function (fstepmodules, ol) {
    'use strict';

    fstepmodules.service('BasketLayerService', ['MapService', '$rootScope', function (MapService, rootScope) {

        var basketStyle = new ol.style.Style({
            fill: new ol.style.Fill({ color: 'rgba(255,128,171,0.2)' }),
            stroke: new ol.style.Stroke({ color: 'rgba(255,64,129,0.6)', width: 2 }),
            image: new ol.style.Circle({
                fill: new ol.style.Fill({
                  color: 'rgba(255,128,171,0.2)'
                }),
                radius: 5,
                stroke: new ol.style.Stroke({
                  color: 'rgba(255,64,129,0.6)',
                  width: 3
                })
            })
        });

        var map = MapService.getMap();


        var basketLayer = new ol.layer.Vector({
            source: new ol.source.Vector({
                features: new ol.Collection()
            }),
            style: basketStyle
        });

        map.addLayer(basketLayer);

        this.loadBasket = function(basketFiles) {
            basketLayer.getSource().clear();
            if(basketFiles && basketFiles.length > 0){

                var parser = new ol.format.GeoJSON();

                for(var i = 0; i < basketFiles.length; i++){
                    var item = basketFiles[i];
                    if(item.metadata && item.metadata.geometry && item.metadata.geometry.coordinates){
                        var resultItem =  new ol.Feature({
                             geometry: parser.readGeometry(item.metadata.geometry, {
                                    dataProjection: 'EPSG:4326',
                                    featureProjection: 'EPSG:3857'
                            }),
                            data: item
                        });
                        basketLayer.getSource().addFeature(resultItem);
                    }
                }
                map.getView().fit(basketLayer.getSource().getExtent(), map.getSize());
            }
        };

        this.unloadBasket = function() {
            basketLayer.getSource().clear();
        };

        this.setVisible = function(visible) {
            basketLayer.setVisible(visible);
        };


        this.selectDatabasketItem = function(item, selected) {
            /*
            var features = basketLayer.getSource().getFeatures();

            for (var i = 0; i < features.length; i++) {
                if((item.id && item.id === features[i].get('data').properties.productIdentifier)){
                    if(selected){
                        selectClick.getFeatures().push(features[i]);
                        map.getView().fit(features[i].getGeometry().getExtent(), map.getSize());
                        var zoomLevel = 3;
                        if(map.getView().getZoom() > 3) {
                            zoomLevel = map.getView().getZoom()-2;
                        }
                        map.getView().setZoom(zoomLevel);
                    } else {
                        selectClick.getFeatures().remove(features[i]);
                    }
                    break;
                }
            }
            */
        };

        this.clear = function() {
            basketLayer.getSource().clear();
        };

    }]);
});
