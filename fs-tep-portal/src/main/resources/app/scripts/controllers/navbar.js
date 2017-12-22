/**
 * @ngdoc function
 * @name fstepApp.controller:NavbarCtrl
 * @description
 * # NavbarCtrl
 * Controller of the navbar
 */
define(['../fstepmodules'], function (fstepmodules) {
    'use strict';

    fstepmodules.controller('NavbarCtrl', ['fstepProperties', '$scope', '$location', 'UserService', 'UserEndpointService', '$window', function (fstepProperties, $scope, $location, UserService, UserEndpointService, $window) {

        $scope.user = undefined;
        $scope.ssoUrl = fstepProperties.SSO_URL;
        $scope.fstepUrl = fstepProperties.FSTEP_URL;
        $scope.analystpUrl = fstepProperties.ANALYST_URL;
        $scope.userEndpoints = [];

        $scope.$watch( function() {
            return UserEndpointService.endpoints;
         }, function( endpoints ) {
            $scope.userEndpoints = endpoints;
         });

        $scope.isActive = function (route) {
            return route === $location.path();
        };

        $scope.user = UserService.params.activeUser;

        $scope.$on('active.user', function(event, user) {
            $scope.user = UserService.params.activeUser;
            /*
            UserEndpointService.getUserEndpoints().then(function(endpoints) {
                $scope.userEndpoints = endpoints;
            });
            */
        });

        $scope.$on('no.user', function() {
            $scope.user = UserService.params.activeUser;
            $scope.userEndpoints = [];
        });

    }]);
});
