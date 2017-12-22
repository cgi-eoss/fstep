/**
 * @ngdoc function
 * @name fstepApp.controller:CommunityManageDatabasketCtrl
 * @description
 * # CommunityManageDatabasketCtrl
 * Controller of the fstepApp
 */

'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('CommunityManageCollectiontCtrl', ['CollectionService', 'FileService', 'CommunityService', '$scope', '$mdDialog', function (CollectionService, FileService, CommunityService, $scope, $mdDialog) {

        /* Get stored Databaskets & Files details */
        $scope.collectionParams = CollectionService.params.community;
        $scope.permissions = CommunityService.permissionTypes;
        $scope.item = "File";


        $scope.itemSearch = {
            searchText: $scope.collectionParams.itemSearchText
        };

        $scope.quickSearch = function (item) {
            if (item.filename && item.filename.toLowerCase().indexOf(
                $scope.itemSearch.searchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };

        $scope.shareQuickSearch = function (item) {
            if (item.group.name.toLowerCase().indexOf(
                $scope.collectionParams.sharedGroupsSearchText.toLowerCase()) > -1) {
                return true;
            }
            return false;
        };


        $scope.refreshCollection = function() {
            CollectionService.refreshSelectedCollection('community');
        };

    }]);
});
