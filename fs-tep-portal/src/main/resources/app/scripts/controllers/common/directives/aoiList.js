define(['../../../fstepmodules'], function (fstepmodules) {
    'use strict';

    fstepmodules.directive('aoiList', ['AoiService', 'AoiLayerService', '$mdSidenav',  function(aoiService, aoiLayerService, $mdSidenav) {
        return {
            scope: {
                aois: '=',
                pagingData: '=',
                aoiGeometryCb: '&',
                selectedAoi: '='
            },
            restrict: 'E',
            link: function(scope, element, attrs) {
                scope.selectAoi = function(aoi) {
                    scope.selectedAoi = aoi;
                    scope.centerAoi(aoi);
                };

                scope.deleteAoi = function(aoi) {
                    aoiService.deleteAoi(aoi.id).then(function() {
                        let idx = scope.aois.indexOf(aoi);
                        scope.aois.splice(idx, 1);
                        scope.selectedAoi = null;
                    });
                };

                scope.centerAoi = function(aoi) {
                    if (!aoi.geometry) {
                        scope.aoiGeometryCb({aoi: aoi}).then(function(geometry) {
                            aoi.geometry = geometry;
                            aoiLayerService.setDrawArea(aoi, true);
                        });
                    }
                    else {
                        aoiLayerService.setDrawArea(aoi, true);
                    }
                };

            },
            templateUrl: 'views/common/directives/aoiList.html'
        };
    }]);
});