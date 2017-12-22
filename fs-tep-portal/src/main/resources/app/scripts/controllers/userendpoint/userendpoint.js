/**
 * @ngdoc function
 * @name fstepApp.controller:UserEndpointCtrl
 * @description
 * # HelpdeskCtrl
 * Controller of the fstepApp
 */
'use strict';
define(['../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('UserEndpointCtrl', ['fstepProperties', '$scope', '$routeParams', '$sce', 'UserEndpointService', function (fstepProperties, $scope, $routeParams, $sce, UserEndpointService) {

        $scope.endpoint = null;

        UserEndpointService.getUserEndpoint($routeParams.app).then(function(endpoint) {
            endpoint.url = $sce.trustAsResourceUrl(endpoint.url);
            $scope.endpoint = endpoint;
        });
    }]);
});

