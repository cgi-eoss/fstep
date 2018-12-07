/**
 * @ngdoc function
 * @name fstepApp.controller:CommunityJobsCtrl
 * @description
 * # CommunityJobsCtrl
 * Controller of the fstepApp
 */

'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('CommunityJobsCtrl', ['JobService', 'CommonService', '$scope', '$rootScope', '$location', '$mdDialog', function (JobService, CommonService, $scope, $rootScope, $location, $mdDialog) {

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

        $scope.cloneJob = function(job){
            JobService.getJobConfig(job).then(function(config){
                //rerun single batch processing subjob
                if (config._embedded.service.type == "PARALLEL_PROCESSOR" && !config.inputs.parallelInputs) {
                    if (config.inputs.input) {
                        config.inputs.parallelInputs = config.inputs.input;
                        delete config.inputs.input;
                    }
                }
                $location.path('/explorer');
                setTimeout(function() {
                    $rootScope.$broadcast('update.selectedService', config._embedded.service, config.inputs, config.label, config._embedded.parent, config.systematicParameter);
                });
            });
        };

        $scope.retryJob = function(job, $event) {

            JobService.estimateRerunCost(job).then(function(estimation){

                var currency = ( estimation.estimatedCost === 1 ? 'coin' : 'coins' );
                CommonService.confirm($event, 'Failed jobs rerun will cost ' + estimation.estimatedCost + ' ' + currency + '.' +
                        '\nAre you sure you want to continue?').then(function (confirmed) {
                    if (confirmed === false) {
                        return;
                    }

                    JobService.retryJob(job).then(function(result){
                        JobService.refreshJobs('community');
                    });
                });
            },
            function (error) {
                if (error.status === 402) {
                    CommonService.infoBulletin($event, 'The cost of this job exceeds your balance. This job cannot be run.' +
                                            '\nYour balance: ' + error.currentWalletBalance + '\nCost estimation: ' + error.estimatedCost);
                } else {
                    CommonService.infoBulletin($event, 'Error retrieving rerun cost estimation. Unable to continue.');
                }
            });

        }

        $scope.cancelJob = function(job){
            JobService.cancelJob(job).then(function(result){
                JobService.refreshJobs('community');
            });
        };


        $scope.terminateJob = function(job){
            JobService.terminateJob(job).then(function(result){
                JobService.refreshJobs('community');
            });
        };

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.jobParams.displayFilters = !$scope.jobParams.displayFilters;
        };

        $scope.getColorForStatus = function(status){
            let style = CommonService.getColor(status);
            if (style) {
                return style.match(/color:\s*[^$;]*/)[0];
            }
        };


    }]);
});
