/**
 * @ngdoc function
 * @name fstepApp.controller:CommunityCtrl
 * @description
 * # CommunityCtrl
 * Controller of the fstepApp
 */
'use strict';

define(['../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('CommunityCtrl', ['$scope', 'CommunityService', 'GroupService', 'UserService', 'ProjectService', 'CollectionService', 'BasketService', 'JobService', 'SystematicService', 'ProductService', 'FileService', 'MessageService', 'TabService', 'CommonService', '$injector', function ($scope, CommunityService, GroupService, UserService, ProjectService, CollectionService, BasketService, JobService, SystematicService, ProductService, FileService, MessageService, TabService, CommonService, $injector) {

        $scope.navInfo = TabService.navInfo.community;
        $scope.bottombarNavInfo = TabService.navInfo.bottombar;

        $scope.groupParams = GroupService.params.community;
        $scope.projectParams = ProjectService.params.community;
        $scope.collectionParams = CollectionService.params.community;
        $scope.basketParams = BasketService.params.community;
        $scope.jobParams = JobService.params.community;
        $scope.systematicParams = SystematicService.params.community;
        $scope.serviceParams = ProductService.params.community;
        $scope.fileParams = FileService.params.community;

        /* Get current user */
        $scope.user = UserService.params.activeUser;

        $scope.$on('active.user', function(event, user) {
            $scope.user = UserService.params.activeUser;
        });
        $scope.$on('no.user', function() {
            $scope.user = UserService.params.activeUser;
        });

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        /* Sidebar navigation */
        $scope.communityTabs = TabService.getCommunityNavTabs();

        function showSidebarArea() {
            $scope.navInfo.sideViewVisible = true;
        }

        $scope.hideSidebarArea = function () {
            $scope.navInfo.sideViewVisible = false;
        };

        $scope.togglePage = function (tab) {
            if($scope.navInfo.activeSideNav === tab && $scope.navInfo.sideViewVisible) {
                $scope.hideSidebarArea();
            } else if($scope.navInfo.activeSideNav === tab && !$scope.navInfo.sideViewVisible) {
                showSidebarArea();
            } else {
                $scope.navInfo.activeSideNav = tab;
                showSidebarArea();
            }
        };

        /** Bottom bar **/
        $scope.displayTab = function(tab){
            $scope.bottombarNavInfo.bottomViewVisible = true;
            $scope.bottombarNavInfo.activeBottomNav = tab;
        };

        $scope.toggleBottomView = function(){
            $scope.bottombarNavInfo.bottomViewVisible = !$scope.bottombarNavInfo.bottomViewVisible;
        };

        /* Sharing */

        /* Share Object Modal */
        $scope.ace = {};
        $scope.shareObjectDialog = function($event, item, type, groups, serviceName, serviceMethod) {
            CommonService.shareObjectDialog($event, item, type, groups, serviceName, serviceMethod, 'community');
        };

        $scope.updateGroups = function (item, type, groups, serviceName, serviceMethod) {
            var service = $injector.get(serviceName);
            CommunityService.updateObjectGroups(item, type, groups).then(function (data) {
                service[serviceMethod]('community');
            });
        };

        $scope.removeGroup = function (item, type, group, groups, serviceName, serviceMethod) {
            var service = $injector.get(serviceName);
            CommunityService.removeAceGroup(item, type, group, groups).then(function (data) {
                service[serviceMethod]('community');
            });
        };

        $scope.displaySidebar = function () {
            if(!$scope.groupParams.selectedGroup && $scope.navInfo.activeSideNav === $scope.communityTabs.GROUPS) {
               return true;
            } else if(!$scope.projectParams.selectedProject && $scope.navInfo.activeSideNav === $scope.communityTabs.PROJECTS) {
                return true;
            } else if(!$scope.basketParams.selectedDatabasket && $scope.navInfo.activeSideNav === $scope.communityTabs.DATABASKETS) {
                return true;
            } else if(!$scope.jobParams.selectedJob && $scope.navInfo.activeSideNav === $scope.communityTabs.JOBS) {
                return true;
            } else if(!$scope.serviceParams.selectedService && $scope.navInfo.activeSideNav === $scope.communityTabs.SERVICES) {
                return true;
            } else if(!$scope.fileParams.selectedFile && $scope.navInfo.activeSideNav === $scope.communityTabs.FILES) {
                return true;
            }
            return false;
        };

        $scope.hideContent = true;
        var navbar, sidenav, groups, projects, collections, databaskets, jobs, systematicprocs, services, servicetemplates, files;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'groups':
                    groups = true;
                    break;
                case 'projects':
                    projects = true;
                    break;
                case 'collections':
                    collections = true;
                    break;
                case 'databaskets':
                    databaskets = true;
                    break;
                case 'jobs':
                    jobs = true;
                    break;
                case 'systematicprocs':
                    systematicprocs = true;
                    break;
                case 'services':
                    services = true;
                    break;
                case 'servicetemplates':
                    servicetemplates = true;
                    break;
                case 'files':
                    files = true;
                    break;
            }

            if (navbar && sidenav && (groups || projects || collections || databaskets || jobs || systematicprocs || services || servicetemplates || files)) {
                $scope.hideContent = false;
            }
        };

    }]);
});
