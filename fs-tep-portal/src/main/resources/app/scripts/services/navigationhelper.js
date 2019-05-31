/**
 * @ngdoc service
 * @name fstepApp.NavigationHelperService
 * @description
 * # NavigationHelperService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules'], function(fstepmodules) {

    fstepmodules.service('NavigationHelperService', ['$http', '$location', 'BasketService', 'JobService', 'FileService', 'TabService', function( $http, $location, BasketService, JobService, FileService, TabService) {

        var navigateToJobsPage = function() {
            JobService.getJobsByFilter('community');
            TabService.navInfo.community.activeSideNav = TabService.getCommunityNavTabs().JOBS;
            $location.path('/community');
        }

        this.goToJob = function(job) {

            JobService.params.community.parentId = null;
            JobService.params.community.selectedOwnershipFilter = JobService.jobOwnershipFilters.ALL_JOBS;
            JobService.params.community.searchText = job.id;
            JobService.params.community.selectedJob = job;

            if (!job.parent) {
                $http.get(job._links.parentJob.href).then(function(response) {
                    JobService.params.community.parentId = response.data.id;
                    navigateToJobsPage();
                }, function() {
                    navigateToJobsPage();
                })
            } else {
                navigateToJobsPage();
            }
        }

        var navigateToFilesPage = function() {
            FileService.getFstepFilesByFilter('community');
            TabService.navInfo.community.activeSideNav = TabService.getCommunityNavTabs().FILES;
            $location.path('/community');
        }

        this.goToFile = function(file) {

            FileService.params.community.selectedOwnershipFilter = FileService.fileOwnershipFilters.ALL_FILES;
            FileService.params.community.searchText = file.filename;
            FileService.params.community.activeFileType = file.type;
            FileService.params.community.collectionSearchString = undefined;
            FileService.params.community.collection = undefined;
            FileService.params.community.job = undefined;
            FileService.params.community.selectedFile = file;

            navigateToFilesPage();

        }

        this.goToBasket = function(basket) {
            BasketService.params.community.displayFilters = true;
            BasketService.params.community.selectedOwnershipFilter = BasketService.dbOwnershipFilters.ALL_BASKETS;
            BasketService.params.community.searchText = basket.name;
            BasketService.params.community.selectedDatabasket = basket;

            BasketService.getDatabasketsByFilter('community');
            TabService.navInfo.community.activeSideNav = TabService.getCommunityNavTabs().DATABASKETS;
            $location.path('/community');
        };

        this.goToCollectionFiles = function(collection) {
            FileService.params.community.selectedOwnershipFilter = FileService.fileOwnershipFilters.ALL_FILES;
            FileService.params.community.searchText = undefined;
            FileService.params.community.activeFileType = 'OUTPUT_PRODUCT';
            FileService.params.community.collectionSearchString = collection.name;
            FileService.params.community.collection = {
                id: collection.id,
                name: collection.name
            }
            FileService.params.community.job = undefined;
            FileService.params.community.selectedFile = undefined;

            navigateToFilesPage();
        }

        this.goToJobFiles = function(job) {
            FileService.params.community.selectedOwnershipFilter = FileService.fileOwnershipFilters.ALL_FILES;
            FileService.params.community.searchText = undefined;
            FileService.params.community.activeFileType = 'OUTPUT_PRODUCT';
            FileService.params.community.collectionSearchString = undefined;
            FileService.params.community.collection = undefined;
            FileService.params.community.job = job.id;
            FileService.params.community.selectedFile = undefined;

            navigateToFilesPage();
        }

        return this;
    }]);
});
