/**
 * @ngdoc function
 * @name fstepApp.controller:CommunityDatabasketsCtrl
 * @description
 * # CommunityDatabasketsCtrl
 * Controller of the fstepApp
 */

'use strict';

define(['../../../fstepmodules'], function (fstepmodules) {

    fstepmodules.controller('CommunityCollectionsCtrl', ['CollectionService', 'NavigationHelperService', 'CommonService', 'PublishingService', 'CostingExpressionService', '$scope', function (CollectionService, NavigationHelperService, CommonService, PublishingService, CostingExpressionService, $scope) {

        $scope.collectionParams = CollectionService.params.community;
        $scope.dbOwnershipFilters = CollectionService.dbOwnershipFilters;
        $scope.fileTypeFilters = CollectionService.fileTypeFilters;
        $scope.item = "Collection";

        CollectionService.refreshCollections("community");

        /* Stop polling */
        $scope.$on("$destroy", function() {
            CollectionService.stopPolling();
        });

        $scope.getPage = function(url){
            CollectionService.refreshCollectionsFromUrl('community', url);
        };

        $scope.filter = function(){
            CollectionService.refreshCollections('community');
        };

        $scope.selectCollection = function (item) {
            $scope.collectionParams.selectedCollection = item;
            CollectionService.refreshSelectedCollection("community");
        };

        $scope.createItemDialog = function ($event) {
            CommonService.createItemDialog($event, 'CollectionService', 'createCollection', 'createcollection', {
                fileTypes: CollectionService.fileTypes
            }).then(function (newCollection) {
                CollectionService.refreshCollections("community", "Create", newCollection);
            });
        };

        $scope.editItemDialog = function ($event, item) {
            CommonService.editItemDialog($event, item, 'CollectionService', 'updateCollection', 'editcollection',  {
                fileTypes: CollectionService.fileTypes
            }).then(function (updatedCollection) {
                CollectionService.refreshCollections("community", updatedCollection._deleted ? 'Remove': 'Update', updatedCollection);
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

        $scope.setDownloadCost = function ($event, collection) {
            CostingExpressionService.showItemCostDialog($event, 'collection', collection.id);
        };

        $scope.gotoCollectionFiles = function(item) {
            NavigationHelperService.goToCollectionFiles(item);
        };

    }]);
});
