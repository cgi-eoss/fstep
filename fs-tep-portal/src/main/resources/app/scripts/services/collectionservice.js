/**
 * @ngdoc service
 * @name fstepApp.CollectionService
 * @description
 * # CollectionService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules', 'traversonHal'], function(fstepmodules, TraversonJsonHalAdapter) {

    fstepmodules.service('CollectionService', ['$rootScope', '$http', 'fstepProperties', '$q', '$timeout', 'MessageService', 'UserService', 'TabService', 'CommunityService', 'FileService', 'traverson', function($rootScope, $http, fstepProperties, $q, $timeout, MessageService, UserService, TabService, CommunityService, FileService, traverson) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = fstepProperties.URLv2;
        var halAPI = traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        /** PRESERVE USER SELECTIONS **/
        this.dbOwnershipFilters = {
            ALL_COLLECTIONS: {id: 0, name: 'All', searchUrl: 'search/findByFilterOnly'},
            MY_COLLECTIONS: {id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner'},
            SHARED_COLLECTIONS: {id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner'}
        };

        this.params = {
            community: {
                pollingUrl: rootUri + '/collections/?sort=name',
                pagingData: {},
                collections: undefined,
                items: undefined,
                selectedCollection: undefined,
                searchText: '',
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                selectedOwnershipFilter: self.dbOwnershipFilters.ALL_COLLECTIONS
            }
        };

        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollCollections = function(page) {
            pollingTimer = $timeout(function() {
                halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function(document) {
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        $rootScope.$broadcast('poll.collections', document._embedded.collections);
                        pollCollections(page);
                    }, function(error) {
                        error.retriesLeft = pollCount;
                        MessageService.addError('Could not poll Collections', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollCollections(page);
                        }
                    });
            }, POLLING_FREQUENCY);
        };

        this.stopPolling = function() {
            if (pollingTimer) {
                $timeout.cancel(pollingTimer);
            }
            startPolling = true;
        };

        var getCollections = function(page) {
            var deferred = $q.defer();
            halAPI.from(self.params[page].pollingUrl)
                .newRequest()
                .getResource()
                .result
                .then(function(document) {
                    if (startPolling) {
                        pollCollections(page);
                        startPolling = false;
                    }
                    self.params[page].pagingData._links = document._links;
                    self.params[page].pagingData.page = document.page;

                    deferred.resolve(document._embedded.collections);
                }, function(error) {
                    MessageService.addError('Could not get Collections', error);
                    deferred.reject();
                });

            return deferred.promise;
        };

        this.createCollection = function(data) {
            return $q(function(resolve, reject) {
                var collection = {name: data.name, description: (data.description ? data.description : ''), productsType: data.productsType};
                halAPI.from(rootUri + '/collections/')
                    .newRequest()
                    .post(collection)
                    .result
                    .then(
                        function(document) {
                            MessageService.addInfo('Collection created', 'New Collection ' + name + ' created.');
                            resolve(JSON.parse(document.data));
                        }, function(error) {
                            MessageService.addError('Could not create Collection ' + name, error);
                            reject();
                        });
            });
        };

        this.removeCollection = function(collection) {
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/collections/' + collection.id)
                    .newRequest()
                    .delete()
                    .result
                    .then(
                        function(document) {
                            if (200 <= document.status && document.status < 300) {
                                MessageService.addInfo('Collection deleted', 'Collection ' + collection.name + ' deleted.');
                                resolve(collection);
                            } else {
                                MessageService.addError('Could not remove Collection ' + collection.name, document);
                                reject();
                            }
                        }, function(error) {
                            MessageService.addError('Could not remove Collection ' + collection.name, error);
                            reject();
                        });
            });
        };

        this.updateCollection = function(collection) {
            var newcollection = {name: collection.name, description: collection.description, productsType: collection.productsType};
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/collections/' + collection.id)
                    .newRequest()
                    .patch(newcollection)
                    .result
                    .then(
                        function(document) {
                            MessageService.addInfo('Collection successfully updated', 'Collection ' + collection.name + ' updated.');
                            resolve(JSON.parse(document.data));
                        }, function(error) {
                            MessageService.addError('Could not update Collection ' + collection.name, error);
                            reject();
                        });
            });
        };

        var getCollection = function(collection) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/collections/' + collection.id + '?projection=detailedCollection')
                .newRequest()
                .getResource()
                .result
                .then(
                    function(document) {
                        deferred.resolve(document);
                    }, function(error) {
                        MessageService.addError('Could not get Collection: ' + collection.name, error);
                        deferred.reject();
                    });
            return deferred.promise;
        };

        this.refreshCollections = function(page, action, collection) {
            if (self.params[page]) {
                /* Get collection list */
                getCollections(page).then(function(data) {

                    self.params[page].collections = data;

                    /* Select last collection if created */
                    if (action === "Create") {
                        self.params[page].selectedCollection = collection;
                    }

                    /* Clear collection if deleted */
                    if (action === "Remove") {
                        if (collection && self.params[page].selectedCollection && collection.id === self.params[page].selectedCollection.id) {
                            self.params[page].selectedCollection = undefined;
                            self.params[page].items = [];
                        }
                    }

                    /* Update the selected collection */
                    self.refreshSelectedCollection(page);
                });
            }
        };

        /* Fetch a new page */
        this.getCollectionsPage = function(page, url) {
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                /* Get databasket list */
                getCollections(page).then(function(data) {
                    self.params[page].collections = data;
                });
            }
        };

        this.getCollectionsByFilter = function(page) {
            if (self.params[page]) {
                var url = rootUri + '/collections/' + self.params[page].selectedOwnershipFilter.searchUrl +
                    '?sort=name&filter=' + (self.params[page].searchText ? self.params[page].searchText : '');

                if (self.params[page].selectedOwnershipFilter !== self.dbOwnershipFilters.ALL_COLLECTIONS) {
                    url += '&owner=' + UserService.params.activeUser._links.self.href;
                }
                self.params[page].pollingUrl = url;

                /* Get databasket list */
                getCollections(page).then(function(data) {
                    self.params[page].collections = data;
                });
            }
        };


        this.refreshSelectedCollection = function(page) {
            if (self.params[page]) {
                /* Get collection contents if selected */
                if (self.params[page].selectedCollection) {

                    getCollection(self.params[page].selectedCollection).then(function(collection) {
                        self.params[page].selectedCollection = collection;

                        if (page === 'community') {
                            CommunityService.getObjectGroups(collection, 'collection').then(function(data) {
                                self.params.community.sharedGroups = data;
                            });
                        }
                    });
                }
            }
        };

        return this;
    }]);
});
