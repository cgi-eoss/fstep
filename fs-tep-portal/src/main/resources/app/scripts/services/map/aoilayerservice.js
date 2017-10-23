/**
 * @ngdoc service
 * @name fstepApp.MapService
 * @description
 * # MapService
 * Service in the fstepApp.
 */
define(['../../fstepmodules', 'ol'], function (fstepmodules, ol) {
    'use strict';

    fstepmodules.service('AoiLayerService', ['MapService', 'AoiService', '$rootScope', function (MapService, aoiService, rootScope) {

        var aoiLayerStyle = new ol.style.Style({
            fill: new ol.style.Fill({ color: 'rgba(188,223,241,0.4)' }),
            stroke: new ol.style.Stroke({ color: '#31708f', width: 2, lineDash: [15, 5] })
        });

        var drawLayerStyle = new ol.style.Style({
            fill: new ol.style.Fill({ color: 'rgba(255,223,0,0.4)' }),
            stroke: new ol.style.Stroke({ color: '#ffff00', width: 2, lineDash: [15, 5] })
        });

        var map = MapService.getMap();

        var aoiLayer = new ol.layer.Vector({
            source: new ol.source.Vector({
                features: new ol.Collection()
            }),
            style: aoiLayerStyle
        });

        map.addLayer(aoiLayer);

        var drawLayer = new ol.layer.Vector({
            source: new ol.source.Vector({
                features: new ol.Collection()
            }),
            style: drawLayerStyle
        });

        map.addLayer(drawLayer);

        var modifyInteraction = new ol.interaction.Modify({
            features: aoiLayer.getSource().getFeaturesCollection(),
            /* the SHIFT key must be pressed to delete vertices, so that new
               vertices can be drawn at the same position of existing vertices */
            deleteCondition: function(event) {
              return ol.events.condition.shiftKeyOnly(event) && ol.events.condition.singleClick(event);
            },
            wrapX: true
        });

        modifyInteraction.on('modifyend',function(evt) {
            rootScope.$apply(function() {
                aoiService.setSearchAoi({
                    geometry: JSON.parse(new ol.format.GeoJSON().writeGeometry(evt.features.item(0).getGeometry(), {
                        dataProjection: 'EPSG:4326',
                        featureProjection: 'EPSG:3857'
                    }))
                });
            });
        });

        map.addInteraction(modifyInteraction);

        var bboxGeometryFunction = function(coordinates, geometry) {
            if (!geometry) {
                geometry = new ol.geom.Polygon(null);
            }
            var start = coordinates[0];
            var end = coordinates[1];
            geometry.setCoordinates([
                [start, [start[0], end[1]], end, [end[0], start[1]], start]
            ]);
            return geometry;
        };

        this.enableDraw = function(type) {

            drawLayer.getSource().clear();

            if (this.drawInteraction) {
                map.removeInteraction(this.drawInteraction);
                delete this.drawInteraction;
            }

            var drawInteraction;
            if (type === 'bbox') {
                drawInteraction = new ol.interaction.Draw({
                    features: drawLayer.getSource().getFeaturesCollection(),
                    type: 'Circle',
                    geometryFunction: bboxGeometryFunction,
                    maxPoints: 2,
                    wrapX: true
                });
            }
            else {
                drawInteraction = new ol.interaction.Draw({
                    features: drawLayer.getSource().getFeaturesCollection(),
                    type: 'Polygon',
                    wrapX: true
                });
            }

            drawInteraction.on('drawend', function (event) {
                map.removeInteraction(drawInteraction);
                rootScope.$apply(function() {
                    /*
                    var area = event.feature.getGeometry().clone().transform('EPSG:3857', 'EPSG:4326')
            
                    //normalize lon to -180/180
                    area.applyTransform(function(coords) {
                        return coords.map(function(coord, idx) {
                            coords[idx] = (idx % 2) ? coord : (coord + 180) % 360 - 180;
                            return coords[idx]
                        });
                    });

                    aoiService.setSearchAoi({
                        name: 'aoi',
                        geometry: JSON.parse(new ol.format.GeoJSON().writeGeometry(area))
                    });
                    */

                    aoiService.setSearchAoi({
                        geometry: JSON.parse(new ol.format.GeoJSON().writeGeometry(event.feature.getGeometry(), {
                            dataProjection: 'EPSG:4326',
                            featureProjection: 'EPSG:3857'
                        }))
                    });
                });

                setTimeout(function() {
                    drawLayer.getSource().clear();
                });
                
            });

            map.addInteraction(drawInteraction);
            this.drawInteraction = drawInteraction;
        };

        this.setDrawArea = function(aoi, centerOnMap) {
            drawLayer.getSource().clear();
            if (aoi && aoi.geometry) {
                var geometry = new ol.format.GeoJSON().readGeometry(aoi.geometry);
                geometry.transform('EPSG:4326', 'EPSG:3857');
                var feature = new ol.Feature({
                    geometry: geometry
                });
                drawLayer.getSource().addFeature(feature);

                if (centerOnMap) {
                    MapService.fitExtent(geometry.getExtent());
                }
            }
        };


        rootScope.$watch(aoiService.getSearchAoi, function(aoi) {
            aoiLayer.getSource().clear();
            if (aoi && aoi.geometry) {
                var geometry = new ol.format.GeoJSON().readGeometry(aoi.geometry);
                geometry.transform('EPSG:4326', 'EPSG:3857');
                var feature = new ol.Feature({
                    geometry: geometry
                });
                aoiLayer.getSource().addFeature(feature);
            }
        });

        this.gotoSearchAoi = function() {
            var aoi = aoiLayer.getSource().getFeatures()[0];
            if (aoi) {
                MapService.fitExtent(aoi.getGeometry().getExtent());
            }
        };

    }]);
});