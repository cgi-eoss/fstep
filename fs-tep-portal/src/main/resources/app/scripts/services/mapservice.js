/**
 * @ngdoc service
 * @name fstepApp.MapService
 * @description
 * # MapService
 * Service in the fstepApp.
 */
define(['../fstepmodules', 'ol'], function (fstepmodules, ol) {
    'use strict';

    fstepmodules.service('MapService', [function () {

        this.searchPolygon = { selectedArea: undefined, wkt: undefined, searchAoi: undefined };
        this.mapType = {active: 'MapBox'};
        this.searchLayerFeatures = new ol.Collection();
        this.resultLayerFeatures = new ol.Collection();
        this.basketLayerFeatures = new ol.Collection();

        this.getPolygonWkt = function(){
            return angular.copy(this.searchPolygon.wkt);
        };

        this.resetSearchPolygon = function(){
            this.searchPolygon.selectedArea = undefined;
            this.searchPolygon.wkt = undefined;
            this.searchPolygon.searchAoi = undefined;
        };

        return this;
    }]);
});
