/**
* @ngdoc function
* @name fstepApp.controller:IndexCtrl
* @description
* # IndexCtrl
* Controller of the fstepApp
*/
define(['../fstepmodules'], function (fstepmodules) {
    'use strict';

    fstepmodules.controller('IndexCtrl', ['$scope', '$location', function ($scope, $location) {

        $scope.goTo = function ( path ) {
            $location.path( path );
        };

        $scope.version = document.getElementById("version").content;

    }]);
});
