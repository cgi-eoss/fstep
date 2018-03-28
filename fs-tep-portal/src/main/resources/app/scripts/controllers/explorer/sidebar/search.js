'use strict';

/**
 * @ngdoc function
 * @name fstepApp.controller:SearchbarCtrl
 * @description
 * # SearchbarCtrl
 * Controller of the fstepApp
 */
define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('SearchbarCtrl', ['$scope', '$rootScope', '$http', 'CommonService', 'BasketService', 'MapService','AoiLayerService', 'SearchService', 'moment', function ($scope, $rootScope, $http, CommonService, BasketService, MapService, AoiLayerService, SearchService, moment) {

        $scope.searchParams = {};
        $scope.selectedCatalog = {};

        $scope.allowedValues = {};

        $scope.enabledFields = {};


        SearchService.getSearchParameters().then(function(data){
            $scope.catalogues = data;
        });

        $scope.getCatalogIcon = function(catalog) {
            if (catalog === 'SATELLITE') {
                return 'satellite';
            } else if (catalog === 'REF_DATA') {
                return 'local_library';
            } else {
                return 'streetview';
            }
        };

        $scope.setDefaultValue = function(field, index) {
            if(field.defaultValue) {
                if(field.type === 'text' || field.type === 'int' || field.type === 'polygon') {
                    $scope.searchParams[index] = field.defaultValue;
                } else if(field.type === 'select') {
                    for (var item in field.allowed.values) {
                        if(field.defaultValue === field.allowed.values[item].value) {
                            $scope.searchParams[index] = field.allowed.values[item].value;
                        }
                    }
                } else if(field.type === 'daterange') {

                    $scope.searchParams[index] = {};

                    var startPeriod = new Date();
                    startPeriod.setMonth(startPeriod.getMonth() + parseInt(field.defaultValue[0]));
                    $scope.searchParams[index].start = startPeriod;

                    var endPeriod = new Date();
                    endPeriod.setMonth(endPeriod.getMonth() + parseInt(field.defaultValue[1]));
                    $scope.searchParams[index].end = endPeriod;
                }
            }
        };

        $scope.selectCatalog = function (field, catalog) {
            $scope.searchParams[field.type] = catalog.value;
            $scope.selectedCatalog = catalog;
            this.getValues();
        };

        $scope.closeCatalog = function (field) {
            $scope.searchParams = {};
            $scope.selectedCatalog = {};
        };

        $scope.pastePolygon = function(identifier){
            $scope.searchParams[identifier] = MapService.getPolygonWkt();
        };

        $scope.clearPolygon = function(identifier){
            MapService.resetSearchPolygon();
        };

        /* If field has no dependencies display it.
         * If field has dependencies, for each find the matching field.
         * Check if any of the condition values matche the current dependency value */
        $scope.displayField = function (field, type) {
            if (field.type === type) {
                if (!field.onlyIf) {
                    return true;
                } else {
                    for (var condition in field.onlyIf) {
                        for (var item in $scope.searchParams) {
                            if (item === condition) {
                                for (var value in field.onlyIf[condition]) {
                                    if(field.onlyIf[condition][value] === $scope.searchParams[item]) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return false;
        };

        $scope.getValues = function() {
            for(var field in $scope.catalogues) {

                // Remove values for fields no longer displayed
                if(!$scope.displayField($scope.catalogues[field], $scope.catalogues[field].type)) {
                    delete $scope.searchParams[field];
                    delete $scope.enabledFields[field];
                }

                $scope.enabledFields[field] = $scope.catalogues[field].optional ? ($scope.enabledFields[field] || false): true;

                if ($scope.catalogues[field].type === 'select') {
                    // Get list of allowed values
                    var allowedValues = getAllowedFields($scope.catalogues[field]);
                    $scope.allowedValues[field] = allowedValues;
                    // Clear any fields set with an invalid value
                    removeInvalidValues(field, allowedValues);
                }
            }
        };

        $scope.getSelectValueDescription = function(field, value) {
            var values = $scope.allowedValues[field];
            for (var i=0; i < values.length; ++i) {
                if (values[i].value == value) {
                    return values[i].description;
                }
            }
        };

        /* For all values*/
        function getAllowedFields(field) {
            var displayValues = [];
            var allFieldValues = field.allowed.values;

            for (var value in allFieldValues) {
                // If value is not dependant on another add to list
                if (!allFieldValues[value].onlyIf) {
                    displayValues.push(allFieldValues[value]);
                // If value depends on anothers
                } else {
                    for (var depField in allFieldValues[value].onlyIf) {
                        var allowedValues = allFieldValues[value].onlyIf[depField];
                        for (var item in allowedValues) {
                            if($scope.searchParams[depField]) {
                                if($scope.searchParams[depField] === allowedValues[item]) {
                                    displayValues.push(allFieldValues[value]);
                                }
                            }
                        }
                    }
                }
            }
            return displayValues;
        }



        function removeInvalidValues(field, allowedValues) {
            if ($scope.catalogues[field]) {
                var match = false;
                for (var v in allowedValues) {
                    if($scope.searchParams[field]) {
                        if($scope.searchParams[field] === allowedValues[v].value) {
                            match = true;
                        }
                    }
                }
                if (!match) {
                    delete $scope.searchParams[field];
                }
            }
        }

        $scope.search = function() {
            var params = {}
            for (var field in $scope.searchParams) {
                if ($scope.enabledFields[field]) {
                    if ($scope.catalogues[field].type == 'daterange') {
                        params[field + 'Start'] = moment($scope.searchParams[field].start).format('YYYY-MM-DD[T00:00:00Z]');
                        params[field + 'End'] = moment($scope.searchParams[field].end).format('YYYY-MM-DD[T23:59:59Z]');
                    }
                    else {
                        params[field] = $scope.searchParams[field];
                    }
                }
            }
            SearchService.submit(params).then(function (searchResults) {
                $rootScope.$broadcast('update.geoResults', searchResults);
            }).catch(function () {
                $rootScope.$broadcast('update.geoResults');
            });
        };

    }]);
});
