/**
 * @ngdoc service
 * @name fstepApp.JobService
 * @description
 * # JobService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules', 'traversonHal', 'moment'], function (fstepmodules, TraversonJsonHalAdapter, moment) {

    fstepmodules.service('JobService', [ 'fstepProperties', '$q', '$timeout', '$rootScope', 'MessageService', 'CommonService', 'UserService', 'CommunityService', 'traverson', function (fstepProperties, $q, $timeout, $rootScope, MessageService, CommonService, UserService, CommunityService, traverson) {

        /* TODO: Migrate self to _this as self is a reserved word and is causing scoping issues */
        var self = this;
        var _this = this;
        var launchedJobID;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = fstepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.jobOwnershipFilters = {
            ALL_JOBS: { id: 0, name: 'All', searchUrl: 'search/findByFilterAndIsNotSubjob', subjobsSearchUrl: 'search/findByFilterAndParent'},
            MY_JOBS: { id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndIsNotSubjobAndOwner', subjobsSearchUrl: 'search/findByFilterAndParentAndOwner' },
            SHARED_JOBS: { id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndIsNotSubjobAndNotOwner', subjobsSearchUrl: 'search/findByFilterAndParentAndNotOwner' }
        };

        this.JOB_STATUSES = [
            { title: "Completed", name: "COMPLETED" },
            { title: "Running", name: "RUNNING" },
            { title: "Error", name: "ERROR" },
            { title: "Created", name: "CREATED" },
            { title: "Pending", name: "PENDING"},
            { title: "Waiting", name: "WAITING"},
            { title: "Cancelled", name: "CANCELLED" }
         ];

        /** PRESERVE USER SELECTIONS **/
        this.params = {
            explorer: {
                jobs: undefined,
                pollingUrl: rootUri + '/jobs?sort=id,DESC',
                pollingRequestOptions: {},
                pagingData: {},
                selectedJob: undefined,
                jobSelectedOutputs: [], //selected outputs
                wms: {
                    isAllVisible: false,
                    visibleList: []
                },
                displayFilters: false, //whether filter section is opened or not
                selectedStatuses: [],
                selectedOwnershipFilter: this.jobOwnershipFilters.MY_JOBS,
                dateFilter: {
                    enabled: false,
                    start: null,
                    end: null
                },
                parentId: null,
                jobCategoryInfo: {} //info about job categories, which ones are opened, etc.
            },
            community: {
                jobs: undefined,
                pollingUrl: rootUri + '/jobs/?sort=id,DESC',
                pollingRequestOptions: {},
                pagingData: {},
                selectedJob: undefined,
                searchText: '',
                displayFilters: false,
                selectedStatuses: [],
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnershipFilter: self.jobOwnershipFilters.MY_JOBS,
                dateFilter: {
                    enabled: false,
                    start: null,
                    end: null
                }
            }
        };
        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollJobs = function (page) {
            pollingTimer = $timeout(function () {
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;
                        $rootScope.$broadcast('poll.jobs', document._embedded.jobs);
                        pollJobs(page);
                    }, function (error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Jobs', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollJobs(page);
                        }
                    }
                );
            }, POLLING_FREQUENCY);
        };

        this.stopPolling = function(){
            if(pollingTimer){
                $timeout.cancel(pollingTimer);
            }
            startPolling = true;
        };

        function getJobs(page) {
            var deferred = $q.defer();
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                 .then(
                function (document) {
                    if(startPolling) {
                        pollJobs(page);
                        startPolling = false;
                    }
                    self.params[page].pagingData._links = document._links;
                    self.params[page].pagingData.page = document.page;

                    deferred.resolve(document._embedded.jobs);
                }, function (error) {
                    MessageService.addError('Could not get Jobs', error);
                    deferred.reject();
                });

            return deferred.promise;
        }

        /* Fetch a new page */
        this.getJobsPage = function(page, url){
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                /* Get jobs list */
                getJobs(page).then(function(data) {
                    self.params[page].jobs = data;
                });
            }
        };

        this.getJobsByFilter = function (page) {
            filterJobs(page);
            self.params[page].jobs = [];
            getJobs(page).then(function (data) {
                self.params[page].jobs = data;
            });
        };

        function filterJobs(page) {

            let params = _this.params[page];

            if (params) {

                let pollingUrl = rootUri + '/jobs/search/parametricFind?sort=id,DESC';

                if (params.parentId) {
                    pollingUrl += '&parentId=' + params.parentId;
                }
                if (params.selectedOwnershipFilter === _this.jobOwnershipFilters.MY_JOBS) {
                    pollingUrl += '&owner=' + UserService.params.activeUser._links.self.href;
                } else if (params.selectedOwnershipFilter === _this.jobOwnershipFilters.SHARED_JOBS) {
                    pollingUrl += '&notOwner=' + UserService.params.activeUser._links.self.href;
                }

                var statusStr = params.selectedStatuses.join(',');
                if (statusStr) {
                    pollingUrl += '&status=' + statusStr;
                }
                if (params.searchText) {
                    pollingUrl += '&filter=' + params.searchText;
                }
                if (params.dateFilter.enabled) {
                    if (params.dateFilter.start) {
                        pollingUrl += '&startDateTime=' + moment(params.dateFilter.start).subtract(params.dateFilter.start.getTimezoneOffset()).format('YYYY-MM-DD[T00:00:00Z]');
                    }
                    if (params.dateFilter.end) {
                        pollingUrl += '&endDateTime=' + moment(params.dateFilter.end).subtract(params.dateFilter.end.getTimezoneOffset()).format('YYYY-MM-DD[T23:59:59.999Z]');
                    }
                }
                if (params.inputFilename) {
                    pollingUrl += '&inputIdentifier=' + params.inputFilename;
                }

                params.pollingUrl = pollingUrl;

            }
        }

        var getJob = function (job) {
            var deferred = $q.defer();
                halAPI.from(rootUri + '/jobs/' + job.id + '?projection=detailedJob')
                         .newRequest()
                         .getResource()
                         .result
                         .then(
                function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get Job ' + job.id, error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        var getJobLogs = function(job) {
            var deferred = $q.defer();

            halAPI.from(rootUri + '/jobs/' + job.id + '/logs')
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get logs for Job ' + job.id, error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        this.getJobConfig = function(job) {
            var deferred = $q.defer();

            halAPI.from(rootUri + '/jobs/' + job.id + '/config')
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get contents of Job ' + job.id, error);
                    deferred.reject();
                });

            return deferred.promise;
        };


        function getOutputFiles(job){
            var deferred = $q.defer();

            halAPI.from(rootUri + '/jobs/' + job.id + '/outputFiles?projection=detailedFstepFile')
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get outputs for Job ' + job.id, error);
                    deferred.reject();
                });

            return deferred.promise;
        }

        this.refreshJobs = function (page, action, job) {

            /* Get job list */
            filterJobs(page);
            getJobs(page).then(function (data) {

                self.params[page].jobs = data;

                /* Select last job if created */
                if (action === "Create") {
                    for (job in self.params[page].jobs) {
                        if (self.params[page].jobs[job].id === launchedJobID) {
                            self.params[page].selectedJob = self.params[page].jobs[job];
                        }
                    }
                }

                /* Update the selected job */
                self.refreshSelectedJob(page);
           });
        };


        this.refreshSelectedJob = function (page) {

            /* Get job contents if selected */
            if (_this.params[page].selectedJob) {

                getJob(_this.params[page].selectedJob).then(function (job) {

                    getJobLogs(job).then(function (logs) {
                        job.logs = logs;
                    });

                    _this.getJobConfig(job).then(function (config) {
                        job.config = config;
                    });

                    if (job.outputs) {
                        getOutputFiles(job).then(function(result){
                            job.outputFiles = result._embedded.fstepFiles;
                            _this.params[page].selectedJob = job;
                            if(page === 'explorer'){
                                _this.params.explorer.jobSelectedOutputs = [];
                            }
                        });
                    } else {
                        _this.params[page].selectedJob = job;
                    }


                    if(page === 'community') {
                        CommunityService.getObjectGroups(job, 'job').then(function (data) {
                            _this.params.community.sharedGroups = data;
                        });
                    }

                });
            }
        };


        this.launchJob = function(jobConfig, service, page) {
            var deferred = $q.defer();
            // Launch the jobConfig
            halAPI.from(jobConfig._links.self.href + '/launch')
                    .newRequest()
                    .post()
                    .result
                    .then(
             function (document) {
                 launchedJobID = JSON.parse(document.data).content.id;
                 MessageService.addInfo('Job ' + launchedJobID + ' started', 'A new ' + service.name + ' job started.');
                 deferred.resolve();
             },
             function(error){
                 MessageService.addError('Could not launch Job', error);
                 deferred.reject();
             });

            return deferred.promise;
        };

        this.createJobConfig = function(service, inputs, label, parent){
            return $q(function(resolve, reject) {
                    var parent_link = null;
                    if (parent){
                        parent_link = parent._links.self.href;
                    }
                    halAPI.from(rootUri + '/jobConfigs/')
                    .newRequest()
                    .post({
                        service: service._links.self.href,
                        inputs: inputs,
                        label: label,
                        parent: parent_link
                    })
                    .result
                    .then(
                 function (document) {
                     resolve(JSON.parse(document.body));
                 }, function (error) {
                     MessageService.addError('Could not create JobConfig', error);
                     reject();
                 });
            });
        };


        this.retryJob = function(job) {
            var deferred = $q.defer();
            // Launch the jobConfig
            halAPI.from(job._links.self.href + '/relaunchFailed')
                    .newRequest()
                    .post()
                    .result
                    .then(
             function (document) {
                 MessageService.addInfo('Job ' + job.id + ' relaunched');
                 deferred.resolve();
             },
             function(error){
                 MessageService.addError('Could not relaunch the Job', error);
                 deferred.reject();
             });

            return deferred.promise;
        }

        this.terminateJob = function(job) {
            var deferred = $q.defer();
            // Launch the jobConfig
            halAPI.from(job._links.terminate.href)
                    .newRequest()
                    .post()
                    .result
                    .then(
             function (document) {
                 MessageService.addInfo('Job ' + job.id + ' cancelled', 'Job ' + job.id + ' terminated by the user.');
                 deferred.resolve();
             },
             function(error){
                 MessageService.addError('Could not terminate the Job', error);
                 deferred.reject();
             });

            return deferred.promise;
        };

        this.cancelJob = function(job) {
            var deferred = $q.defer();
            // Launch the jobConfig
            halAPI.from(job._links.cancel.href)
                    .newRequest()
                    .get()
                    .result
                    .then(
             function (document) {
                 MessageService.addInfo('Job ' + job.id + ' cancelled', 'Job ' + job.id + ' cancelled by the user.');
                 deferred.resolve();
             },
             function(error){
                 MessageService.addError('Could not cancel the Job', error);
                 deferred.reject();
             });

            return deferred.promise;
        };

        return this;
    }]);
});
