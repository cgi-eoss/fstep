/**
 * @ngdoc function
 * @name fstepApp.controller:ServiceCtrl
 * @description
 * # ServiceCtrl
 * Controller of the fstepApp
 */
'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('DeveloperServicesCtrl', ['$scope', 'ProductService', 'ProductTemplateService', 'UserMountsService', 'CommonService', '$mdDialog', function ($scope, ProductService, ProductTemplateService, UserMountsService, CommonService, $mdDialog) {

        $scope.serviceParams = ProductService.params.development;
        $scope.serviceOwnershipFilters = ProductService.serviceOwnershipFilters;
        $scope.serviceTypeFilters = ProductService.serviceTypeFilters;

        $scope.serviceTypes = {
            APPLICATION: { id: 0, name: 'Application', value: 'APPLICATION'},
            PROCESSOR: { id: 0, name: 'Processor', value: 'PROCESSOR'},
            BULK_PROCESSOR: { id: 0, name: 'Bulk Processor', value: 'BULK_PROCESSOR'},
            PARALLEL_PROCESSOR: { id: 0, name: 'Parallel Processor', value: 'PARALLEL_PROCESSOR'}
        };


        $scope.toggleServiceFilter = function(){
            $scope.serviceParams.displayFilters = !$scope.serviceParams.displayFilters;
        };

        ProductService.refreshServices('development');

        $scope.selectService = function(service) {
            $scope.serviceParams.displayRight = true;
            $scope.serviceParams.selectedService = service;
            ProductService.refreshSelectedService('development');
        };

        /* Update Services when polling */
        $scope.$on('poll.services', function (event, data) {
            $scope.serviceParams.services = data;
        });

        $scope.$on("$destroy", function() {
            ProductService.stopPolling();
        });

        /* Paging */
        $scope.getPage = function(url){
            ProductService.getServicesPage('development', url);
        };

        $scope.filter = function(){
            ProductService.getServicesByFilter('development');
        };

        $scope.removeService = function(event, service){
            CommonService.confirm(event, 'Are you sure you want to delete this service: "' + service.name + '"?').then(function (confirmed){
                if(confirmed === false){
                    return;
                }
                ProductService.removeService(service).then(function(){
                    ProductService.refreshServices('development', 'Remove', service);
                });
            });
        };

        $scope.createService = function($event) {
            function CreateServiceController($scope, $mdDialog) {

                $scope.serviceTypes = [];

                $scope.serviceTemplates = {}
                $scope.defaultTemplate = {}

                for (var id in ProductService.serviceTypeFilters) {
                    if (ProductService.serviceTypeFilters[id].value) {
                        $scope.serviceTypes.push({
                            name: ProductService.serviceTypeFilters[id].name,
                            value: ProductService.serviceTypeFilters[id].value
                        });
                    }
                }

                $scope.setDefaultServiceTemplate = function() {

                    if (!$scope.defaultTemplate[$scope.newItem.serviceType]) {
                        ProductTemplateService.getDefaultTemplate($scope.newItem.serviceType).then(function(response) {
                            if (response) {
                                $scope.defaultTemplate[$scope.newItem.serviceType] = response;
                                $scope.newItem.serviceTemplate = response.id;
                            }
                        });
                    } else {
                        $scope.newItem.serviceTemplate = $scope.defaultTemplate[$scope.newItem.serviceType].id;
                    }
                }

                $scope.updateServiceTemplates = function() {

                    if (!$scope.serviceTemplates[$scope.newItem.serviceType]) {

                        var request = ProductTemplateService.getServiceTemplates({
                            serviceType: $scope.newItem.serviceType
                        }).then(function(response) {
                            $scope.serviceTemplates[$scope.newItem.serviceType] = response.data;
                        });

                        $scope.serviceTemplates[$scope.newItem.serviceType] = request;

                    }

                    return $scope.serviceTemplates[$scope.newItem.serviceType];
                }

                $scope.createService = function () {
                    ProductTemplateService.createService($scope.newItem).then(function (newService) {
                        ProductService.refreshServices('development', 'Create', newService);
                    });
                    $mdDialog.hide();
                };

                $scope.closeDialog = function () {
                    $mdDialog.hide();
                };
            }

            CreateServiceController.$inject = ['$scope', '$mdDialog'];
            $mdDialog.show({
                controller: CreateServiceController,
                templateUrl: 'views/developer/templates/createservice.tmpl.html',
                parent: angular.element(document.body),
                targetEvent: $event,
                clickOutsideToClose: true
            });
        };

    }]);

});
