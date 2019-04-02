/**
 * @ngdoc function
 * @name fstepApp.controller:WorkspaceCtrl
 * @description
 * # WorkspaceCtrl
 * Controller of the fstepApp
 */
'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('WorkspaceCtrl', [ '$scope', 'JobService', 'SystematicService', 'EstimateCostService', 'ProductService', 'SearchService', 'MapService', 'CommonService', function ($scope, JobService, SystematicService, EstimateCostService, ProductService, SearchService, MapService, CommonService) {

        $scope.serviceParams = ProductService.params.explorer;
        $scope.runModes = ProductService.serviceRunModes;
        $scope.isWorkspaceLoading = false;

        $scope.searchForm = {
            config: {},
            api: {},
            data: {
                catalogue: 'SATELLITE'
            }
        }

        SearchService.getSearchParameters().then(function(data){

            delete data.productDate;
            delete data.catalogue;

            var config = {
                productDateStart: {
                    type: 'date',
                    defaultValue: '0',
                    description: 'UTC',
                    title: 'Product start date'
                },
                productDateEnd: {
                    type: 'date',
                    defaultValue: '0',
                    description: 'UTC',
                    optional: true,
                    format: 'YYYY-MM-DD[T23:59:59Z]',
                    title: 'Product end date'
                }
            }

            delete data.catalogue;

            $scope.searchForm.config = Object.assign(config, data);
        });

        $scope.$on('update.selectedService', function(event, service, inputs, label, parent, systematicParameter) {
            $scope.isWorkspaceLoading = true;
            $scope.serviceParams.inputValues = {};
            $scope.serviceParams.label = label;
            $scope.serviceParams.parent = parent;
            $scope.serviceParams.dropLists = {};
            if(inputs){
                for (var key in inputs) {
                    $scope.serviceParams.inputValues[key] = inputs[key][0]; //First value is the actual input

                    //if value has links in it, add also to dropList to show file chips
                    if(inputs[key][0].indexOf('://') > -1){
                        var list = inputs[key][0].split(',');
                        $scope.serviceParams.dropLists[key] = [];
                        for(var index in list){
                            $scope.serviceParams.dropLists[key].push({ link: list[index] });
                        }
                    }
                }
            }

            ProductService.getService(service).then(function(detailedService){
                $scope.serviceParams.selectedService = detailedService;
                if (detailedService.type !== 'APPLICATION') {
                    if (systematicParameter) {
                        $scope.serviceParams.runMode = $scope.runModes.SYSTEMATIC.id;
                        $scope.serviceParams.systematicParameter = systematicParameter;
                    }
                    else if ($scope.serviceParams.runMode === $scope.runModes.SYSTEMATIC.id) {
                        $scope.serviceParams.systematicParameter = detailedService.serviceDescriptor.dataInputs[0].id;
                    }
                }
                else {
                    $scope.serviceParams.runMode = $scope.runModes.STANDARD.id;
                    delete $scope.serviceParams.systematicParameter;
                }
                $scope.isWorkspaceLoading = false;
            });
        });

        $scope.onRunModeChange = function() {
            if ($scope.serviceParams.runMode === $scope.runModes.SYSTEMATIC.id) {
                $scope.serviceParams.systematicParameter = $scope.serviceParams.selectedService ? $scope.serviceParams.selectedService.serviceDescriptor.dataInputs[0].id : null;
            }
            else {
                delete $scope.serviceParams.systematicParameter;
            }
        }

        $scope.getDefaultValue = function(fieldDesc){
            return $scope.serviceParams.inputValues[fieldDesc.id] ? $scope.serviceParams.inputValues[fieldDesc.id] : fieldDesc.defaultAttrs.value;
        };


        $scope.launchProcessing = function($event) {
            var iparams={};

            for(var key in $scope.serviceParams.inputValues){
                var value = $scope.serviceParams.inputValues[key];
                if(value === undefined){
                    value = '';
                }
                iparams[key] = [value];
            }

            if ($scope.serviceParams.runMode === $scope.runModes.SYSTEMATIC.id) {
                delete iparams[$scope.serviceParams.systematicParameter];

                var searchParams = $scope.searchForm.api.getFormData();
                searchParams.catalogue = 'SATELLITE';

                EstimateCostService.estimateSystematicCost(
                    $scope.serviceParams.selectedService,
                    $scope.serviceParams.systematicParameter,
                    iparams,
                    searchParams
                ).then(function(estimation) {

                    EstimateCostService.showCostDialog($event, estimation, function() {
                        $scope.displayTab($scope.bottomNavTabs.JOBS, false);

                        SystematicService.launchSystematicProcessing($scope.serviceParams.selectedService, $scope.serviceParams.systematicParameter, iparams, searchParams, $scope.serviceParams.label).then(function () {
                            JobService.refreshJobs("explorer", "Create");
                        });

                    });

                }, function(error) {
                    EstimateCostService.showCostDialog($event, error);
                });

            }
            else {
                JobService.createJobConfig($scope.serviceParams.selectedService, iparams, $scope.serviceParams.label,  $scope.serviceParams.parent).then(function(jobConfig){
                    EstimateCostService.estimateJob(jobConfig).then(function(estimation){

                        EstimateCostService.showCostDialog($event, estimation, function() {
                            $scope.displayTab($scope.bottomNavTabs.JOBS, false);
                            JobService.launchJob(jobConfig, $scope.serviceParams.selectedService, 'explorer').then(function () {
                                JobService.refreshJobs("explorer", "Create");
                            });
                        });
                    },
                    function (error) {
                        EstimateCostService.showCostDialog($event, error);
                    });
                });
            }
        };

        /** DRAG-AND-DROP FILES TO THE INPUT FIELD **/
        $scope.onDrop = function(dropObject, fieldId) {
            if($scope.serviceParams.dropLists[fieldId] === undefined){
                $scope.serviceParams.dropLists[fieldId] = [];
            }

            var file = {};

            if(dropObject && dropObject.type === 'outputs') {
                for(var i = 0; i < dropObject.selectedOutputs.length; i++){
                    file = {
                        name: dropObject.selectedOutputs[i]._links.fstep.href,
                        link: dropObject.selectedOutputs[i]._links.fstep.href,
                        start: dropObject.job.startTime,
                        stop: dropObject.job.endTime
                    };
                    if($scope.serviceParams.inputValues[fieldId] === undefined || $scope.serviceParams.inputValues[fieldId].indexOf(file.link) < 0){
                        $scope.serviceParams.dropLists[fieldId].push(file);
                    }
                }
                setFilesInputString(fieldId);
            }
            else if(dropObject && dropObject.type === 'results') {
                for(var j = 0; j < dropObject.selectedItems.length; j++){
                    file = {
                        name: dropObject.selectedItems[j].properties._links.fstep.href,
                        link: dropObject.selectedItems[j].properties._links.fstep.href,
                        start: dropObject.selectedItems[j].properties.extraParams.fstepStartTime,
                        stop: dropObject.selectedItems[j].properties.extraParams.fstepEndTime,
                        bytes: dropObject.selectedItems[j].properties.filesize
                    };
                    if($scope.serviceParams.inputValues[fieldId] === undefined || $scope.serviceParams.inputValues[fieldId].indexOf(file.link) < 0){
                        $scope.serviceParams.dropLists[fieldId].push(file);
                    }
                }
                setFilesInputString(fieldId);
            }
            else if(dropObject && dropObject.type === 'databasket') {
                file = {
                    name: "Databasket: " + dropObject.basket.name,
                    link: dropObject.basket._links.fstep.href
                };
                if($scope.serviceParams.inputValues[fieldId] === undefined || $scope.serviceParams.inputValues[fieldId].indexOf(file.link) < 0){
                    $scope.serviceParams.dropLists[fieldId].push(file);
                }
                setFilesInputString(fieldId);
                return true;
            }
            else if(dropObject && dropObject.type === 'basketItems') {
                for(var k = 0; k < dropObject.selectedItems.length; k++) {
                    file = {
                        name: dropObject.selectedItems[k]._links.fstep.href,
                        link: dropObject.selectedItems[k]._links.fstep.href
                    };
                    if($scope.serviceParams.inputValues[fieldId] === undefined || $scope.serviceParams.inputValues[fieldId].indexOf(file.link) < 0){
                        $scope.serviceParams.dropLists[fieldId].push(file);
                    }
                }
                setFilesInputString(fieldId);
            }
            else {
                return false;
            }
            return true;
        };

        function setFilesInputString(fieldId){
            var pathsStr = '';
            for(var i = 0; i < $scope.serviceParams.dropLists[fieldId].length; i++){
                pathsStr += ',' + $scope.serviceParams.dropLists[fieldId][i].link;
            }
            $scope.serviceParams.inputValues[fieldId] = pathsStr.substring(1);
        }

        $scope.removeSelectedItem = function(fieldId, item){
            var index = $scope.serviceParams.dropLists[fieldId].indexOf(item);
            $scope.serviceParams.dropLists[fieldId].splice(index, 1);
            setFilesInputString(fieldId);
        };

        $scope.updateDropList = function(fieldId){
            if($scope.serviceParams.inputValues[fieldId] && $scope.serviceParams.inputValues[fieldId].indexOf('://') > -1) {
                var csvList = $scope.serviceParams.inputValues[fieldId].split(',');
                if($scope.serviceParams.dropLists[fieldId] === undefined){
                    $scope.serviceParams.dropLists[fieldId] = [];
                }
                var newDropList = [];
                for(var index in csvList){
                    var exists = false;
                    for(var i in $scope.serviceParams.dropLists[fieldId]){
                        if($scope.serviceParams.dropLists[fieldId][i].link === csvList[index]){
                            newDropList.push($scope.serviceParams.dropLists[fieldId][i]);
                            exists = true;
                        }
                    }
                    if(!exists && csvList[index] !== ''){
                        newDropList.push({link: csvList[index]});
                    }
                }
                $scope.serviceParams.dropLists[fieldId] = newDropList;
            } else {
                $scope.serviceParams.dropLists[fieldId] = undefined;
            }
        };

    }]);
});
