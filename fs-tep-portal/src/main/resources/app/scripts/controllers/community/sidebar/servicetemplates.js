/**
 * @ngdoc function
 * @name fstepApp.controller:CommunityServiceTemplatesCtrl
 * @description
 * # CommunityServicesCtrl
 * Controller of the fstepApp
 */

'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('CommunityServiceTemplatesCtrl', ['ProductTemplateService', 'DeveloperTemplatesCtrlState', 'PublishingService', 'CommonService', 'TabService', '$scope', '$mdDialog', function (ProductTemplateService, DeveloperTemplatesCtrlState, PublishingService, CommonService, TabService, $scope, $mdDialog) {

        /* Get stored Service details */
        $scope.serviceParams = DeveloperTemplatesCtrlState.params;
        $scope.serviceOwnershipFilters = ProductTemplateService.templateOwnershipFilters;
        $scope.serviceTypeFilters = ProductTemplateService.templateTypeFilters;
        $scope.publicationFilters = ProductTemplateService.templatePublicationFilters;
        $scope.item = "Service template";

        /* Get Services */
        DeveloperTemplatesCtrlState.refreshTemplates();


        /* Paging */
        $scope.getPage = function(url){
            ProductTemplateService.getServicesFromUrl(url);
        };

        $scope.filter = function(){
            DeveloperTemplatesCtrlState.refreshTemplates();
        };

        /* Select a Service */
        $scope.selectService = function (item) {
            DeveloperTemplatesCtrlState.selectTemplate(item);
        };

        /* Filters */
        $scope.toggleFilters = function () {
            $scope.serviceParams.displayFilters = !$scope.serviceParams.displayFilters;
        };

        /* Remove Service */
        $scope.removeServiceItem = function (key, item) {
            DeveloperTemplatesCtrlState.removeTemplate(service);
        };

        /* Edit Service */
        $scope.editItemDialog = function ($event, item) {
            CommonService.editItemDialog($event, item, 'ProductTemplateService', 'saveTemplate').then(function (updatedTemplate) {
                DeveloperTemplatesCtrlState.refreshTemplates();
            });
        };

        $scope.gotoDevPage = function(template) {
            TabService.navInfo.developer.activeSideNav = TabService.getDeveloperSideNavs().TEMPLATES;
            DeveloperTemplatesCtrlState.selectTemplate(template);
            $scope.goTo('/developer');
        }

        /* Publication */
        $scope.requestPublication = function ($event, template) {
            CommonService.confirm($event, 'Do you wish to publish this Template?').then(function (confirmed) {
                if (confirmed !== false) {
                    PublishingService.requestPublication(template, 'ServiceTemplate').then(function (data) {
                        DeveloperTemplatesCtrlState.refreshTemplates();
                    });
                }
            });
        };

        $scope.publishTemplate = function ($event, template) {
            PublishingService.publishItemDialog($event, template, 'serviceTemplates', function() {
                DeveloperTemplatesCtrlState.refreshTemplates();
            });
        };

    }]);
});
