/**
* @ngdoc function
* @name fstepApp.controller:IndexCtrl
* @description
* # IndexCtrl
* Controller of the fstepApp
*/
define(['../fstepmodules'], function (fstepmodules) {
    'use strict';

    fstepmodules.controller('IndexCtrl', ['fstepProperties', '$scope', '$location', '$window', 'UserService', function (fstepProperties, $scope, $location, $window, UserService) {

        $scope.fstepUrl = fstepProperties.FSTEP_URL;
        $scope.sessionEnded = false;
        $scope.timeoutDismissed = false;

        $scope.$on('no.user', function() {
            $scope.sessionEnded = true;
        });

        $scope.hideTimeout = function() {
            $scope.sessionEnded = false;
            $scope.timeoutDismissed = true;
        };

        $scope.reloadRoute = function() {
            $window.location.reload();
        };

        $scope.goTo = function ( path ) {
            $location.path( path );
        };

        $scope.version = document.getElementById("version").content;

        // Trigger a user check to ensure controllers load correctly
        UserService.checkLoginStatus();
    }]);
});
