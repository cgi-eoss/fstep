/**
 * @ngdoc service
 * @name fstepApp.MapService
 * @description
 * # MapService
 * Service in the fstepApp.
 */
define(['../../fstepmodules', 'ol'], function (fstepmodules, ol) {
    'use strict';

    fstepmodules.service('ResultLayerService', ['MapService', '$rootScope', function (MapService, rootScope) {

        var resultStyle = new ol.style.Style({
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

        var resultsLayer = new ol.layer.Vector({
            source: new ol.source.Vector({
                features: new ol.Collection()
            }),
            style: resultStyle
        });

        map.addLayer(resultsLayer);


        var selectClick = new ol.interaction.Select({
            condition: ol.events.condition.click,
            toggleCondition: ol.events.condition.shiftKeyOnly,
            style: new ol.style.Style({
                fill: new ol.style.Fill({
                    color: 'rgba(174,213,129,0.8)'
                }),
                stroke: new ol.style.Stroke({
                    color: 'rgba(85,139,47,0.8)',
                    width: 3
                }),
                image: new ol.style.Circle({
                    fill: new ol.style.Fill({
                        color: 'rgba(174,213,129,0.8)'
                    }),
                    radius: 5,
                    stroke: new ol.style.Stroke({
                        color: 'rgba(85,139,47,0.8)',
                        width: 3
                    })
                })
            }),
            layers: [resultsLayer]
        });

        selectClick.on('select', function (evt) {
            var selectedItems = [];
            for (var i = 0; i < selectClick.getFeatures().getLength(); i++) {
                if (selectClick.getFeatures().item(i) && selectClick.getFeatures().item(i).get('data')) {
                    selectedItems.push(selectClick.getFeatures().item(i).get('data'));
                }
            }
            rootScope.$broadcast('map.item.toggled', selectedItems);
        });

        map.addInteraction(selectClick);


        this.selectItem = function(item, selected) {
            var features = resultsLayer.getSource().getFeatures();
            for (var i in features) {
                var feature = features[i];
                if (item.id && item.id === feature.get('data').id) {
                    if (selected) {
                        selectClick.getFeatures().push(feature);
                        var geometry = feature.getGeometry();
                        if (geometry) {
                            map.getView().fit(geometry.getExtent(), map.getSize()); //center the map to the selected vector
                            var zoomLevel = 3;
                            if (geometry instanceof ol.geom.Point) {
                                zoomLevel = 6;
                            }
                            else if (map.getView().getZoom() > 3) {
                                zoomLevel = map.getView().getZoom() - 2;
                            }
                            map.getView().setZoom(zoomLevel); //zoom out a bit, to show the location better
                        }
                    }
                    else {
                        selectClick.getFeatures().remove(feature);
                    }
                    break;
                }
            }
        };

        this.selectAll = function(selected) {
            if (resultsLayer) {
                selectClick.getFeatures().clear();
                if (selected) {
                    for (var i = 0; i < resultsLayer.getSource().getFeatures().length; i++) {
                        selectClick.getFeatures().push(resultsLayer.getSource().getFeatures()[i]);
                    }
                }
            }
        };

        this.selectItems = function(items) {
            this.selectAll(false);
            for (var i = 0; i < items.length; i++) {
                this.selectItem(items[i], true);
            }
        };

        this.setResults = function (results) {
            this.selectAll(false);
            resultsLayer.getSource().clear();
            if (results && results.features && results.features.length > 0) {
                var zoomToPlace = false;
                var parser = new ol.format.GeoJSON();
                for (var result in results.features) {
                    var item = results.features[result];
                    var resultItem = new ol.Feature({
                        data: item
                    });
                    if (item.geometry) {

                        var geometry = parser.readGeometry(item.geometry, {
                            dataProjection: 'EPSG:4326',
                            featureProjection: 'EPSG:3857'
                        });
                        var geomExtent = geometry.getExtent();

                        if (geomExtent.every(n => isFinite(n))) {
                            resultItem.setGeometry(geometry);
                            zoomToPlace = true;
                        }

                    }
                    resultsLayer.getSource().addFeature(resultItem);
                }
                if (zoomToPlace) {
                    map.getView().fit(resultsLayer.getSource().getExtent(), map.getSize(), {
                        maxZoom: 10
                    });
                }
            }
        };

        this.setVisible = function (visible) {
            resultsLayer.setVisible(visible);
        };

        this.clear = function () {
            resultsLayer.getSource().clear();
        };

    }]);
});
