/**
 * @ngdoc function
 * @name fstepApp.controller:DeveloperTemplatesCtrl
 * @description
 * # ServiceCtrl
 * Controller of the fstepApp
 */
'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {


    fstepmodules.service('DeveloperTemplatesCtrlState', ['ProductTemplateService', 'CommunityService', '$q', function ( ProductTemplateService, CommunityService, $q ) {

        var self = this;

        this.params = {
            services: undefined,
            pagingData: {},
            activeForm: undefined,
            displayFilters: false,
            displayRight: false,
            selectedService: undefined,
            selectedOwnershipFilter: ProductTemplateService.templateOwnershipFilters.ALL_SERVICES,
            selectedTypeFilter: ProductTemplateService.templateTypeFilters.ALL_SERVICES,
            selectedServiceFileTab: 1,
            contentsSearchText: '',
            contentsDisplayFilters: false,
            sharedGroups: undefined,
            sharedGroupsSearchText: '',
            sharedGroupsDisplayFilters: false,
            searchText: '',
            fileTree: undefined,
            openedFile: undefined,
            activeMode: undefined
        };


        this.getFileList =  function()  {

            var files = self.params.selectedService.files;
            var filename;
            var list = [];
            for (var file in files) {
                var indent = 0;
                filename = files[file].filename;
                while(filename.indexOf('/') !== -1) {
                    var folderexists = false;
                    for(var i=0; i < list.length; i++) {
                        if(list[i].name.indexOf(filename.slice(0, filename.indexOf("/")))  !== -1) {
                             folderexists = true;
                        }
                    }
                    if(!folderexists) {
                       list.push({name: filename.slice(0, filename.indexOf("/")), type: 'folder', indent: indent});
                    }
                    filename = filename.substring(filename.indexOf("/") + 1);
                    indent++;
                }
                list.push({name: filename, type: 'file', indent: indent, contents: files[file]});
            }

            var previousIndent = 0;
            var nextIndent;
            for(var item = 0; item < list.length; item++) {
                var currentIndent = list[item].indent;

                if(list.length > item + 1) {
                    nextIndent = list[item + 1].indent;
                } else {
                    nextIndent = 'end';
                }

                if(nextIndent === 'end' && currentIndent === 0) {
                    list[item].tree = "└─";
                } else if(currentIndent === 0) {
                    list[item].tree="├";
                } else {
                    list[item].tree="│";
                    for(var j = 0; j < currentIndent; j++) {
                        if (j < currentIndent -1) {
                            list[item].tree = list[item].tree + "...";
                            if(currentIndent > 0) {
                               list[item].tree = list[item].tree + "│";  //Needs forward logic to check if │ or ...
                            }
                        } else {
                            list[item].tree = list[item].tree + "...";
                            if(nextIndent === 'end') {
                                list[item].tree = list[item].tree + "└─";
                            } else if(currentIndent === nextIndent) {
                                list[item].tree = list[item].tree + "├─";
                            } else if(currentIndent < nextIndent) {
                                list[item].tree = list[item].tree + "├─"; //Needs forward logic to check if ├─ or └─
                            } else if(currentIndent > nextIndent) {
                                list[item].tree = list[item].tree + "└─";
                            }
                        }
                    }
                }
                previousIndent = currentIndent;
            }

             self.params.fileTree = list;
        };

        this.setFileType = function () {
            if (!self.params.openedFile) {
                return;
            }
            var filename = self.params.openedFile.filename;
            var extension = filename.slice((filename.lastIndexOf(".") - 1 >>> 0) + 2).toLowerCase();
            var modes = ['Text', 'Dockerfile', 'Javascript', 'Perl', 'PHP', 'Python', 'Properties', 'Shell', 'XML', 'YAML' ];

            if (filename === "Dockerfile") {
                self.params.activeMode = modes[1];
            } else {
                switch(extension) {
                    case "js":
                        self.params.activeMode = modes[2];
                        break;
                    case "pl":
                        self.params.activeMode = modes[3];
                        break;
                    case "php":
                        self.params.activeMode = modes[4];
                        break;
                     case "py":
                        self.params.activeMode = modes[5];
                        break;
                    case "properties":
                        self.params.activeMode = modes[6];
                        break;
                    case "sh":
                        self.params.activeMode = modes[7];
                        break;
                    case "xml":
                        self.params.activeMode = modes[8];
                        break;
                    case "yml":
                        self.params.activeMode = modes[9];
                        break;
                    default:
                        self.params.activeMode = modes[0];
                }
            }
        };

        this.removeTemplate = function(template) {
            ProductTemplateService.removeTemplate(template).then(function(){
                var templates = self.params.services;
                for (var i=0; i < templates.length; ++i) {
                    if (templates[i].id === template.id) {
                        templates.splice(i, 1);
                        break;
                    }
                }

                if (self.params.selectedService.id === template.id) {
                    self.params.selectedService = undefined;
                }

            });
        }

        this.refreshTemplates = function() {
            return ProductTemplateService.getServiceTemplates({
                owner: this.params.selectedOwnershipFilter.id,
                serviceType: this.params.selectedTypeFilter.value,
                searchText: this.params.searchText
            }).then(function(response) {
                self.params.pagingData = response.paging;
                self.params.services = response.data;
            });
        }

        this.selectTemplate = function(template) {
            this.params.displayRight = true;
            this.params.selectedService = template;
            ProductTemplateService.getTemplateDetails(template).then(function(templateDetails) {
                self.params.selectedService = templateDetails;

                ProductTemplateService.getTemplateFiles(templateDetails).then(function(templateFiles) {

                    var promises = [];
                    for(var i = 0; i < templateFiles.length; i++){
                        var partialPromise = ProductTemplateService.getFileDetails(templateFiles[i]);
                        promises.push(partialPromise);
                    }
                    $q.all(promises).then(function(files) {

                        self.params.selectedService.files = files;
                        self.params.openedFile = self.params.selectedService.files[0];
                        self.getFileList();
                        self.setFileType();

                    });

                    self.params.selectedService.files = templateFiles;
                    self.getFileList();

                });

                CommunityService.getObjectGroups(template, 'serviceTemplate').then(function (data) {
                    self.params.sharedGroups = data;
                });
            });
        }

        this.refreshSelected = function() {
            this.selectTemplate(this.params.selectedService);
        }


        return this;
    }]);


    fstepmodules.controller('DeveloperTemplatesCtrl', ['$scope', 'ProductService', 'ProductTemplateService', 'DeveloperTemplatesCtrlState', 'UserMountsService', 'CommonService', '$mdDialog', '$q', function ($scope, ProductService, ProductTemplateService, DeveloperTemplatesCtrlState, UserMountsService, CommonService, $mdDialog, $q) {

        $scope.serviceParams = DeveloperTemplatesCtrlState.params;
        $scope.serviceOwnershipFilters = ProductTemplateService.templateOwnershipFilters;
        $scope.serviceTypeFilters = ProductTemplateService.templateTypeFilters;

        $scope.serviceTypes = {
            APPLICATION: { id: 0, name: 'Application', value: 'APPLICATION'},
            PROCESSOR: { id: 0, name: 'Processor', value: 'PROCESSOR'},
            PARALLEL_PROCESSOR: { id: 0, name: 'Parallel Processor', value: 'PARALLEL_PROCESSOR'}
        };


        $scope.toggleServiceFilter = function(){
            $scope.serviceParams.displayFilters = !$scope.serviceParams.displayFilters;
        };

        $scope.refreshServices = function() {
            DeveloperTemplatesCtrlState.refreshTemplates();
        };

        DeveloperTemplatesCtrlState.refreshTemplates();

        $scope.selectService = function(template) {
            DeveloperTemplatesCtrlState.selectTemplate(template);
        };

        /* Paging */
        $scope.getPage = function(url){
            ProductTemplateService.getServicesFromUrl(url);
        };

        $scope.filter = function(){
            $scope.refreshServices();
        };

        $scope.removeService = function(event, service){
            CommonService.confirm(event, 'Are you sure you want to delete this template: "' + service.name + '"?').then(function (confirmed){
                if(confirmed === false){
                    return;
                }
                DeveloperTemplatesCtrlState.removeTemplate(service);
            });
        };

        $scope.createService = function($event) {

                var parentScope = $scope;

                function CreateServiceController($scope, $mdDialog) {

                    $scope.serviceTypes = [];

                    $scope.servicesForType = {}

                    for (var id in ProductTemplateService.templateTypeFilters) {
                        if (ProductTemplateService.templateTypeFilters[id].value) {
                            $scope.serviceTypes.push({
                                name: ProductTemplateService.templateTypeFilters[id].name,
                                value: ProductTemplateService.templateTypeFilters[id].value
                            });
                        }
                    }

                    $scope.resetServiceFrom = function() {
                        $scope.newItem.fromService = 'none';
                    }
                    $scope.updateServicesForType = function() {


                        if (!$scope.servicesForType[$scope.newItem.serviceType]) {

                            var request = ProductTemplateService.getServicesForType($scope.newItem.serviceType).then(function(response) {
                                $scope.servicesForType[$scope.newItem.serviceType] = response.data;
                            });

                            $scope.servicesForType[$scope.newItem.serviceType] = request;

                        }

                        return $scope.servicesForType[$scope.newItem.serviceType];

                    }

                    $scope.createTemplate = function () {
                        if ($scope.newItem.fromService !== 'none') {
                            ProductTemplateService.createTemplateFromService($scope.newItem).then(function (newTemplate) {
                                parentScope.refreshServices();
                                parentScope.selectService(newTemplate);
                            });
                        }
                        else {
                            ProductTemplateService.createTemplate($scope.newItem).then(function (newTemplate) {
                                parentScope.refreshServices();
                                parentScope.selectService(newTemplate);
                            });
                        }
                        $mdDialog.hide();
                    };

                    $scope.closeDialog = function () {
                        $mdDialog.hide();
                    };
                }

                CreateServiceController.$inject = ['$scope', '$mdDialog'];
                $mdDialog.show({
                    controller: CreateServiceController,
                    templateUrl: 'views/developer/templates/createservicetemplate.tmpl.html',
                    parent: angular.element(document.body),
                    targetEvent: $event,
                    clickOutsideToClose: true
                });
        };

    }]);

});
