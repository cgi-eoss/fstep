/**
 * @ngdoc function
 * @name fstepApp.controller:CommunityJobsCtrl
 * @description
 * # CommunityJobsCtrl
 * Controller of the fstepApp
 */

'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('CommunitySystematicProcessingsCtrl', ['SystematicService', 'CommonService', 'TabService', 'NavigationHelperService', '$scope', '$http', '$mdDialog', function (SystematicService, CommonService, TabService, NavigationHelperService, $scope, $http, $mdDialog) {

        $scope.systematicParams = SystematicService.params.community;
        $scope.ownershipFilters = SystematicService.ownershipFilters;
        $scope.item = "Systematic processings";

        SystematicService.refreshSystematicProcessings('community');

        $scope.$on('poll.systematicprocs', function (event, data) {
            $scope.systematicParams.systematicProcessings = data;
        });

        $scope.$on("$destroy", function() {
            SystematicService.stopPolling();
        });

        $scope.filter = function() {
            SystematicService.getSystematicProcessingsByFilter('community');
        }

        $scope.getPage = function(url){
            SystematicService.getSystematicProcessingsPage('community', url);
        };

        $scope.selectSystematicProcessing = function (item) {
            $scope.systematicParams.selectedSystematicProcessing = item;
            SystematicService.refreshSelectedSystematicProcessing('community');
        };

        $scope.goToParentJobPage = function(systematicProcessing) {
            $http.get(systematicProcessing._links.parentJob.href).then(function(response) {
                NavigationHelperService.goToJob(response.data);
            }, function() {

            });
        };

        /* Remove File */
        $scope.terminateSystematicProcessing = function (event, key, item) {
            CommonService.confirm(event, 'Are you sure you want to terminate this systematic processing?').then(function (confirmed) {
                if (confirmed !== false) {
                    SystematicService.terminateSystematicProcessing(item).then(function() {
                        $scope.selectSystematicProcessing(null);
                    });
                }
            });
        };

    }]);
});
