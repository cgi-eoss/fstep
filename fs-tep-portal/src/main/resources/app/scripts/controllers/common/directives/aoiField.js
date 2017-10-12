define(['../../../fstepmodules', 'shp'], function (fstepmodules, shp) {
    'use strict';

    fstepmodules.directive('aoiField', ['AoiService', 'AoiLayerService', 'CommonService', '$mdDialog', '$mdPanel', function(aoiService, aoiLayerService, commonService, $mdDialog, $mdPanel) {
        return {
            scope: {
                label: '=',
                description: '=',
                value: '='
            },
            restrict: 'E',
            link: function(scope, element, attrs) {
                
                scope.savedAoi = {
                    pagingData: {},
                    records: [],
                    getGeometry: function(aoi) {
                        return aoiService.getSavedAoiGeometry(aoi);
                    },
                    selected: null
                }

                scope.importAoi = {
                    records: [],
                    selected: null
                }


                scope.$watch(aoiService.getSearchAoi, function(aoi) {
                    scope.value = aoi ? new ol.format.WKT().writeGeometry(new ol.format.GeoJSON().readGeometry(aoi.geometry)) : '';
                });

                scope.enableMapAoiDraw = function(type) {
                    aoiLayerService.enableDraw(type);
                }

                scope.clearAoi = function() {
                    aoiService.setSearchAoi(null);
                }

                scope.saveAoi = function($event) {

                    var aoi = aoiService.getSearchAoi()
                    if (!aoi) {
                        return;
                    }

                    let saveDialog = $mdDialog.prompt()
                        .title('Save Area')
                        .placeholder('Name')
                        .ariaLabel('Name')
                        .initialValue(aoi.name)
                        .targetEvent($event)
                        .ok('Save')
                        .cancel('Cancel');
                    
                    $mdDialog.show(saveDialog).then(function(name) {
                        if (name) {
                            aoiService.saveAoi(name, aoi.geometry).then(function() {

                            }, function(error) {
                                if (error == 'Conflict') {
                                    let duplicateDialog = $mdDialog.confirm()
                                        .title('Duplicate name')
                                        .textContent('An area with the same name already exists. Overwrite?')
                                        .ok('Yes')
                                        .cancel('No');
        
                                    $mdDialog.show(duplicateDialog).then(function() {
                                        aoiService.updateAoi(name, aoi.geometry);
                                    }, function() {
        
                                    });
                                }
                            })
                        }
                    }, function() {

                    })
                }

                scope.editAoi = function($event) {
                    scope.editPolygonDialog($event, scope.value);
                }

                scope.copyToClipboard = function() {
                    clipboard.copy(scope.value);
                }

                scope.editPolygonDialog = function($event, wkt) {
                    function EditPolygonController($scope, $mdDialog, MapService) {
                        
                        $scope.polygon = { wkt: wkt, valid: false};
                        

                        $scope.closeDialog = function() {
                            $mdDialog.hide();
                        };
                        $scope.updatePolygon = function(wkt){
                            if(wkt && wkt !== ''){
                                var polygon = new ol.format.WKT().readGeometry(wkt);
                                aoiService.setSearchAoi({
                                    geometry: JSON.parse(new ol.format.GeoJSON().writeGeometry(polygon))
                                });
                            }
                            $mdDialog.hide();
                        };
                        $scope.validateWkt = function(wkt){
                            try{
                                new ol.format.WKT().readGeometry(wkt);
                                $scope.polygon.valid = true;
                            }
                            catch(error){
                                $scope.polygon.valid = false;
                            }
                        };

                        $scope.validateWkt(wkt);
                    }
                    EditPolygonController.$inject = ['$scope', '$mdDialog', 'MapService'];
                    $mdDialog.show({
                        controller: EditPolygonController,
                        templateUrl: 'views/explorer/templates/editpolygon.tmpl.html',
                        parent: angular.element(document.body),
                        targetEvent: $event,
                        clickOutsideToClose: true
                    });
                };

                var showAlert = function(title, message) {
                    $mdDialog.show(
                        $mdDialog.alert()
                          .clickOutsideToClose(true)
                          .title(title)
                          .textContent(message)
                          .ariaLabel(title)
                          .ok('OK')
                      );
                }
                var handleImportedPolygons = function(geomes) {
                    var polygons = geomes.filter(function(geom) {
                        return geom.type == 'Polygon';
                    })

                    if (!polygons.length) {
                        showAlert('Import error', 'No polygon found in the selected shapefile.');
                        return;
                    }

                    let idx = 1;

                    scope.$apply(function() {
                        scope.importAoi = {
                            records : polygons.map(function(geom) {
                                return {
                                    geometry: geom,
                                    name: 'aoi_' + idx++
                                }
                            })
                        };
                    })

                    scope.importAoi.selected = null;
                    scope.loadAoiDialog(null, scope.importAoi);

                }
                
                element.find('#shpfile-in').bind('change', function(event) {
                    var file = event.target.files[0];
                    if (file) {
                        var reader = new FileReader();

                        reader.onload = function(evt) {
                            try {
                                if (/zip/.test(file.type)) {
                                    shp(reader.result).then(function(geojson) {
                                        handleImportedPolygons(geojson.features.map(function(feature) {
                                            return feature.geometry;
                                        }));
                                    }, function(error) {
                                        showAlert('Import error', 'Error parsing shapefile.');
                                    })
                                }
                                else {
                                    var features = shp.parseShp(reader.result);
                                    handleImportedPolygons(features);
                                }
                            }
                            catch(e) {
                                showAlert('Import error', 'Error parsing shapefile.');
                            }
                        }

                        reader.readAsArrayBuffer(file);
                    }

                    event.target.value = '';
                });

                scope.loadArea = function($event) {
                    aoiService.getSavedAois().then(function(response) {
                        scope.savedAoi.pagingData = response.pagingData;
                        scope.savedAoi.records = response.records;
                        scope.savedAoi.selected = null;
                        if (!response.records.length) {
                            showAlert('Error', 'No saved area found');
                        }
                        else {
                            scope.loadAoiDialog($event, scope.savedAoi);
                        }
                    });
                }

                scope.loadAoiDialog = function($event, aoiData) {

                    var position = $mdPanel.newPanelPosition()
                        .absolute()
                        .center();

                    var dialog = null;

                    function LoadAoiController($scope) {

                        $scope.aoiData = aoiData;
                        $scope.closeDialog = function() {
                            dialog.close().then(function() {
                                dialog.destroy();
                            });
                        };

                    }
                    LoadAoiController.$inject = ['$scope'];
                    $mdPanel.open({
                        controller: LoadAoiController,
                        templateUrl: 'views/common/directives/aoiLoadDialog.html',
                        attachTo: element,
                        targetEvent: $event,
                        hasBackdrop: true,
                        clickOutsideToClose: false,
                        position: position
                    }).then(function(instance) {
                        dialog = instance;
                    });
                };

                
                scope.loadAoiDialog = function($event, aoiData) {
                    
                    function LoadAoiController($scope, $mdDialog, AoiService, AoiLayerService) {

                        $scope.aoiData = aoiData;
                        $scope.closeDialog = function() {
                            AoiLayerService.setDrawArea(null);
                            $mdDialog.hide();
                        };

                        $scope.loadAoi = function(aoi) {
                            AoiLayerService.setDrawArea(null);
                            AoiService.setSearchAoi(aoi);
                            $mdDialog.hide();
                        }

                    }
                    LoadAoiController.$inject = ['$scope', '$mdDialog', 'AoiService', 'AoiLayerService'];
                    $mdDialog.show({
                        controller: LoadAoiController,
                        templateUrl: 'views/common/directives/aoiLoadDialog.html',
                        parent: element,
                        targetEvent: $event,
                        hasBackdrop: true,
                        clickOutsideToClose: false
                    })
                };

                
            },
            templateUrl: 'views/common/directives/aoiField.html'
        }
    }])
});