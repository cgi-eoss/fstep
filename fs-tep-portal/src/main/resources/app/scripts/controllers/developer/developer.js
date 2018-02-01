/**
 * @ngdoc function
 * @name fstepApp.controller:DeveloperCtrl
 * @description
 * # DeveloperCtrl
 * Controller of the fstepApp
 */
'use strict';

define(['../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('DeveloperCtrl', ['$scope', 'TabService', 'MessageService', 'CommonService', function ($scope, TabService, MessageService, CommonService) {

        $scope.developerSideNavs = TabService.getDeveloperSideNavs();
        $scope.navInfo = TabService.navInfo.developer;
        $scope.bottombarNavInfo = TabService.navInfo.bottombar;

        /* Active session message count */
        $scope.message = {};
        $scope.message.count = MessageService.countMessages();
        $scope.$on('update.messages', function(event, job) {
            $scope.message.count = MessageService.countMessages();
        });

        $scope.getColorForStatus = function(status){
            return CommonService.getColor(status);
        };

        $scope.toggleBottomView = function(){
            $scope.bottombarNavInfo.bottomViewVisible = !$scope.bottombarNavInfo.bottomViewVisible;
        };

        function showSidebarArea() {
            $scope.navInfo.sideViewVisible = true;
        }

        $scope.hideSidebarArea = function () {
            $scope.navInfo.activeSideNav = undefined;
            $scope.navInfo.sideViewVisible = false;
        };

        $scope.toggleSidebar = function (tab) {
            if($scope.navInfo.activeSideNav === tab) {
                $scope.hideSidebarArea();
            } else {
                $scope.navInfo.activeSideNav = tab;
                showSidebarArea();
            }
        };

        $scope.hideContent = true;
        var navbar, sidenav, services;
        $scope.finishLoading = function(component) {
            switch(component) {
                case 'navbar':
                    navbar = true;
                    break;
                case 'sidenav':
                    sidenav = true;
                    break;
                case 'services':
                    services = true;
                    break;
            }

            if (navbar && sidenav && services) {
                $scope.hideContent = false;
            }
        };

    }]);

});
