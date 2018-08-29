/**
 * @ngdoc function
 * @name fstepApp.controller:CommunityDatabasketsCtrl
 * @description
 * # CommunityDatabasketsCtrl
 * Controller of the fstepApp
 */

'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('CommunityCollectionsCtrl', ['CollectionService', 'CommonService', 'PublishingService', '$scope', function (CollectionService, CommonService, PublishingService, $scope) {

        $scope.collectionParams = CollectionService.params.community;
        $scope.dbOwnershipFilters = CollectionService.dbOwnershipFilters;
        $scope.item = "Collection";

        CollectionService.refreshCollections("community");

        $scope.$on('poll.collections', function (event, data) {
            $scope.collectionParams.collections = data;
        });

        /* Stop polling */
        $scope.$on("$destroy", function() {
            CollectionService.stopPolling();
        });

        $scope.getPage = function(url){
            CollectionService.getCollectionsPage('community', url);
        };

        $scope.filter = function(){
            CollectionService.getCollectionsByFilter('community');
        };

        $scope.selectCollection = function (item) {
            $scope.collectionParams.selectedCollection = item;
            CollectionService.refreshSelectedCollection("community");
        };

        $scope.createItemDialog = function ($event) {
            CommonService.createItemDialog($event, 'CollectionService', 'createCollection', 'createcollection').then(function (newCollection) {
                CollectionService.refreshCollections("community", "Create", newCollection);
            });
        };

        $scope.editItemDialog = function ($event, item) {
            CommonService.editItemDialog($event, item, 'CollectionService', 'updateCollection', 'editcollection').then(function (updatedCollection) {
                CollectionService.refreshCollections("community");
            });
        };

        $scope.requestPublication = function ($event, collection) {
            CommonService.confirm($event, 'Do you wish to publish this collection?').then(function (confirmed) {
                if (confirmed !== false) {
                    PublishingService.requestPublication(collection, 'Collection').then(function (data) {
                        CollectionService.refreshCollections("community");
                    });
                }
            });
        };

        $scope.publishCollection = function ($event, collection) {
            PublishingService.publishItemDialog($event, collection, 'collections', function() {
                CollectionService.refreshCollections("community");
            });
        };

    }]);
});
