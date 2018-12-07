/**
 * @ngdoc function
 * @name fstepApp.controller:CommunityJobsCtrl
 * @description
 * # CommunityJobsCtrl
 * Controller of the fstepApp
 */

'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('CommunityJobsCtrl', ['JobService', '$scope', '$mdDialog', function (JobService, $scope, $mdDialog) {

        /* Get stored Jobs details */
        $scope.jobParams = JobService.params.community;
        $scope.jobOwnershipFilters = JobService.jobOwnershipFilters;
        $scope.item = "Job";
        $scope.jobStatuses = JobService.JOB_STATUSES;

        /* Get jobs */
        JobService.refreshJobs('community');

        /* Update jobs when polling */
        $scope.$on('poll.jobs', function (event, data) {
            $scope.jobParams.jobs = data;
        });

        /* Paging */
        $scope.getPage = function(url){
            JobService.getJobsPage('community', url);
        };

        var pendingRefresh = null;

        $scope.filter = function(){

            if (pendingRefresh) {
                clearTimeout(pendingRefresh);
            }
            pendingRefresh = setTimeout(function() {
                JobService.getJobsByFilter('community');
            }, 500);
        };

        /* Stop Polling */
        $scope.$on("$destroy", function() {
            if (pendingRefresh) {
                clearTimeout(pendingRefresh);
            }
            JobService.stopPolling();
        });

        /* Select a Job */
        $scope.selectJob = function (item) {
            $scope.jobParams.selectedJob = item;
            JobService.refreshSelectedJob('community');
        };

        $scope.setParentJobFilter = function(job) {
            $scope.jobParams.parentId = job ? job.id : null;
            $scope.jobParams.searchText = '';
            $scope.jobParams.dateFilter.enabled = false;
            $scope.jobParams.inputFilename = '';
            JobService.getJobsByFilter('community');
        }


        /* Filters */
        $scope.toggleFilters = function () {
            $scope.jobParams.displayFilters = !$scope.jobParams.displayFilters;
        };

    }]);
});
