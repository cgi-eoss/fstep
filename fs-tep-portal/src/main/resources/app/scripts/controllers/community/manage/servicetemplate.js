/**
 * @ngdoc function
 * @name fstepApp.controller:CommunityManageServiceTemplateCtrl
 * @description
 * # CommunityServiceCtrl
 * Controller of the fstepApp
 */

'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('CommunityManageServiceTemplateCtrl', ['CommunityService', 'ProductTemplateService', 'DeveloperTemplatesCtrlState', 'TabService', '$scope', function (CommunityService, ProductTemplateService, DeveloperTemplatesCtrlState, TabService, $scope) {

        /* Get stored Services & Contents details */
        $scope.serviceParams = DeveloperTemplatesCtrlState.params;
        $scope.permissions = CommunityService.permissionTypes;
        $scope.item = "Service template file";

        /* Filters */
        $scope.toggleContentsFilters = function () {
            $scope.serviceParams.displayContentsFilters = !$scope.serviceParams.displayContentsFilters;
        };

        $scope.contentsSearch = {
            searchText: $scope.serviceParams.contentsSearchText
        };

        $scope.contentsQuickSearch = function (item) {
            if (item.filename.toLowerCase().indexOf(
                $scope.contentsSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.toggleSharingFilters = function () {
            $scope.serviceParams.sharedGroupsDisplayFilters = !$scope.serviceParams.sharedGroupsDisplayFilters;
        };

        $scope.quickSharingSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.serviceParams.sharedGroupsSearchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.refreshService = function() {
            DeveloperTemplatesCtrlState.selectTemplate($scope.serviceParams.selectedService);
        };

        $scope.gotoDevPage = function() {
            TabService.navInfo.developer.activeSideNav = TabService.getDeveloperSideNavs().TEMPLATES;
            DeveloperTemplatesCtrlState.selectTemplate($scope.serviceParams.selectedService);
            $scope.goTo('/developer');
        }

        /* Remove file from service */
        $scope.removeServiceItem = function(files, file) {
            ProductService.removeServiceItem($scope.projectParams.selectedProject, files, file).then(function (data) {
                ProductService.refreshServices("community");
                /* TODO: Implement removeServiceItem in ProductService */
            });
        };

    }]);
});
