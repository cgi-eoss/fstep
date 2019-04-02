/**
 * @ngdoc function
 * @name fstepApp.controller:CommunityManageJobCtrl
 * @description
 * # CommunityManageJobCtrl
 * Controller of the fstepApp
 */

'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('CommunityManageJobCtrl', ['CommunityService', 'JobService', 'FileService', '$scope', function (CommunityService, JobService, FileService, $scope) {

        /* Get stored Jobs details */
        $scope.jobParams = JobService.params.community;
        $scope.permissions = CommunityService.permissionTypes;
        $scope.item = "Job";

        /* Filters */
        $scope.toggleSharingFilters = function () {
            $scope.jobParams.sharedGroupsDisplayFilters = !$scope.jobParams.sharedGroupsDisplayFilters;
        };

        $scope.quickSharingSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.jobParams.sharedGroupsSearchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.refreshJob = function() {
            JobService.refreshSelectedJob('community');
        };

        $scope.splitInputFiles = function(link) {
            return link.split(',');
        };

        $scope.estimateDownloadCost = function($event, file){
            FileService.downloadFile($event, file);
        };


    }]);
});

