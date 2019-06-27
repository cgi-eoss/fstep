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
            ALL_COLLECTIONS: {id: 0, name: 'All'},
            MY_COLLECTIONS: {id: 1, name: 'Mine'},
            SHARED_COLLECTIONS: {id: 2, name: 'Shared'}
        };

        this.fileTypes = [
            {name: 'Output product', value: 'OUTPUT_PRODUCT'},
            {name: 'Reference data', value: 'REFERENCE_DATA'}
        ]

        this.fileTypeFilters = this.fileTypes.slice();
        this.fileTypeFilters.unshift({
            name: 'All', value: ''
        });

        this.params = {
            community: {
                pagingData: {},
                collections: undefined,
                selectedCollection: undefined,
                searchParams: {
                    ownership: self.dbOwnershipFilters.ALL_COLLECTIONS,
                    fileType: null,
                    searchText: null
                },
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
            }
        };

        var polling = {
            frequency: 20 * 1000,
            remainingAttempts: 3,
            pollTimeout: null
        }

        var buildSearchUrlFromParams = function(params) {

            var url = rootUri + '/collections/search/parametricFind?sort=name';

            if (params.ownership === self.dbOwnershipFilters.MY_COLLECTIONS) {
                url += '&owner=' + UserService.params.activeUser._links.self.href
            } else if (params.ownership === self.dbOwnershipFilters.SHARED_COLLECTIONS) {
                url += '&notOwner=' + UserService.params.activeUser._links.self.href
            }

            if (params.fileType) {
                url += '&fileType=' + params.fileType;
            }

            if (params.searchText) {
                url += '&filter=' + params.searchText;
            }

            return url;
        }

        var updateCollectionsForState = function(state) {
            var deferred = $q.defer();
            halAPI.from(state.pollingUrl)
                .newRequest()
                .getResource()
                .result
                .then(function(document) {

                    state.pagingData = {
                        _links: document._links,
                        page: document.page
                    }
                    state.collections = document._embedded.collections;

                    deferred.resolve(document);
                }, function(error) {
                    MessageService.addError('Could not get Collections', error);
                    deferred.reject();
                });

            return deferred.promise;
        }

        var setPollingTimeout = function(state) {
            polling.pollTimeout = $timeout(function() {
                updateCollectionsForState(state).then(function() {
                    setPollingTimeout(state);
                }, function(error) {
                    if (polling.remainingAttempts) {
                        polling.remainingAttempts--;
                        setPollingTimeout(state);
                    }
                });
            }, polling.frequency);
        }

        this.refreshCollections = function(page, action, collection) {

            var state = self.params[page];

            if (state) {

                self.stopPolling();

                state.pollingUrl = buildSearchUrlFromParams(state.searchParams);

                /* Get collection list */
                updateCollectionsForState(state).then(function(data) {

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

                }).finally(function() {
                    setPollingTimeout(state);
                });

            }
        }

        this.refreshSelectedCollection = function(page) {

            var state = self.params[page];

            if (state) {
                /* Get collection contents if selected */
                if (state.selectedCollection) {

                    getCollection(state.selectedCollection).then(function(collection) {
                        state.selectedCollection = collection;

                        if (page === 'community') {
                            CommunityService.getObjectGroups(collection, 'collection').then(function(data) {
                                state.sharedGroups = data;
                            });
                        }
                    });
                }
            }
        };

        this.refreshCollectionsFromUrl = function(page, url) {

            var state = self.params[page];

            if (state) {
                state.pollingUrl = url;
                updateCollectionsForState(state);
            }
        };


        this.stopPolling = function() {
            if (polling.pollTimeout) {
                $timeout.cancel(polling.pollTimeout);
                delete polling.pollTimeout;
            }
        };

        this.createCollection = function(data) {
            return $q(function(resolve, reject) {
                var collection = {
                    name: data.name,
                    description: (data.description ? data.description : ''),
                    fileType: data.fileType,
                    productsType: data.productsType
                };
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

        this.findCollections = function(params) {
            var url = buildSearchUrlFromParams(params);

            return halAPI.from(url)
                .newRequest()
                .getResource()
                .result
                .then(function(document) {
                    return document._embedded.collections;
                });
        }

        return this;
    }]);
});
