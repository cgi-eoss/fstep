/**
* @ngdoc function
* @name fstepApp.controller:MapCtrl
* @description
* # MapCtrl
* Controller of the fstepApp
*/
define(['../../fstepmodules', 'ol', 'x2js', 'clipboard'], function (fstepmodules, ol, X2JS, clipboard) {
    'use strict';

    fstepmodules.controller('MapCtrl', [ '$scope', '$element', '$rootScope', '$mdDialog', 'fstepProperties', 'MapService', 'AoiService', 'AoiLayerService', 'ResultLayerService', 'BasketLayerService', 'ProductLayerService', 'SearchService', '$timeout', function($scope, $element, $rootScope, $mdDialog, fstepProperties, MapService, aoiService, aoiLayer, resultLayer, basketLayer, productLayer, searchService, $timeout) {
        //$element.append(MapService.getMap().getTargetElement());

        $scope.backgroundLayer = MapService.getBackgroundLayer();

        var map = MapService.getMap();

        setTimeout(function() {
            map.setTarget($element.get(0));
        }, 0);


        $scope.setBackgroundLayer = function(layer) {
            MapService.setBackgroundLayer(layer);
        };

        $scope.clearMap = function() {
            aoiService.setSearchAoi(null);
            searchService.params.geoResults = [];
            resultLayer.clear();
            basketLayer.clear();
            productLayer.clear();
            map.getView().setZoom(4);
            map.getView().setCenter(ol.proj.fromLonLat([0, 51.28]));
            $rootScope.$broadcast('map.cleared');
        };

        $scope.$on('update.geoResults', function(event, results) {
            resultLayer.setResults(results);
        });

        $scope.$on('results.select.all', function (event, selected) {
            resultLayer.selectAll(selected);
        });

        $scope.$on('results.item.selected', function (event, item, selected) {
            resultLayer.selectItem(item, selected);
        });

        $scope.$on('results.invert', function (event, items) {
            resultLayer.selectItems(items);
        });

        $scope.$on('load.basket', function(event, basketFiles) {
            resultLayer.setVisible(false);
            basketLayer.loadBasket(basketFiles);
            basketLayer.setVisible(true);
        });

        $scope.$on('unload.basket', function(event) {
            resultLayer.setVisible(true);
            basketLayer.setVisible(false);
        });

        $scope.$on('databasket.item.selected', function(event, item, selected) {
            basketLayer.selectDatabasketItem(item, selected);
        });

        $scope.$on('update.wmslayer', function(event, files) {
            productLayer.setWMSLayers(files);
        });
        

    }]);
});
