/**
 * @ngdoc function
 * @name fstepApp.controller:CommunityJobsCtrl
 * @description
 * # CommunityJobsCtrl
 * Controller of the fstepApp
 */

'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('CommunityJobsCtrl', ['JobService', 'EstimateCostService', 'CommonService', '$scope', '$rootScope', '$location',  function (JobService, EstimateCostService, CommonService, $scope, $rootScope, $location) {

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

            EstimateCostService.estimateJobRerun(job).then(
                function(response) {
                    EstimateCostService.showCostDialog($event, response, function() {
                        JobService.retryJob(job).then(function(result){
                            JobService.refreshJobs('community');
                        });
                    })
                },
                function (error) {
                    EstimateCostService.showCostDialog($event, error)
                }
            );

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

        $scope.formatJobStatus = function(job) {
            if (job.status !== 'WAITING') {
                return job.status;
            } else if (typeof(job.queuePosition) === 'number' && job.queuePosition !== -1) {
                var lastChar = job.queuePosition.toString().substr(-1);
                var postfix;
                switch(lastChar) {
                    case '1':
                        postfix = 'st';
                        break;
                    case '2':
                        postfix = 'nd';
                        break;
                    case '3':
                        postfix = 'rd';
                        break;
                    default:
                        postfix = 'th';
                }

                return job.queuePosition + postfix + ' in queue';
            } else {
                return 'QUEUED';
            }
        }


    }]);
});
