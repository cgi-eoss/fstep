/**
 * @ngdoc service
 * @name fstepApp.FileService
 * @description
 * # FileService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules', 'traversonHal'], function (fstepmodules, TraversonJsonHalAdapter) {

    fstepmodules.service('FileService', [ 'fstepProperties', '$q', 'MessageService', 'UserService', 'CommunityService', 'EstimateCostService', 'traverson', '$rootScope', '$timeout', 'Upload', function (fstepProperties, $q, MessageService, UserService, CommunityService, EstimateCostService, traverson, $rootScope, $timeout, Upload) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = fstepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.fileOwnershipFilters = {
                ALL_FILES: { id: 0, name: 'All'},
                MY_FILES: { id: 1, name: 'Mine'},
                SHARED_FILES: { id: 2, name: 'Shared' }
        };

        /** PRESERVE USER SELECTIONS **/
        this.params = {
            community: {
                pollingUrl: undefined,
                pagingData: {},
                files: undefined,
                fileDetails: undefined,
                selectedFile: undefined,
                activeFileType: "REFERENCE_DATA",
                collection: undefined,
                job: undefined,
                searchText: '',
                displayFilters: false,
                sharedGroups: undefined,
                sharedGroupsSearchText: '',
                sharedGroupsDisplayFilters: false,
                selectedOwnershipFilter: self.fileOwnershipFilters.ALL_FILES,
                progressPercentage: 0,
                uploadStatus: 'pending',
                uploadMessage: undefined
             }
        };
        /** END OF PRESERVE USER SELECTIONS **/

        var POLLING_FREQUENCY = 20 * 1000;
        var pollCount = 3;
        var startPolling = true;
        var pollingTimer;

        var pollFstepFiles = function (page) {
            pollingTimer = $timeout(function () {
                var request = halAPI.from(self.params[page].pollingUrl)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function (document) {
                        self.params[page].pollingUrl = document._links.self.href;
                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        $rootScope.$broadcast('poll.fstepfiles', document._embedded.fstepFiles);
                        pollFstepFiles(page);
                     }, function (error) {
                        MessageService.addError('Could not get Files', error);
                        if (pollCount > 0) {
                            pollCount -= 1;
                            pollFstepFiles(page);
                        }
                    });
            }, POLLING_FREQUENCY);
        };

        this.stopPolling = function(){
            if(pollingTimer){
                $timeout.cancel(pollingTimer);
            }
            startPolling = true;
        };

        /* File types: REFERENCE_DATA, OUTPUT_PRODUCT, EXTERNAL_PRODUCT */
        this.getFstepFiles = function (page, url) {
            if(url){
                self.params[page].pollingUrl = url;
            }

            var deferred = $q.defer();
            var request = /* Get files list */
                halAPI.from(self.params[page].pollingUrl)
                .newRequest()
                .getResource()
                .result
                .then(function (document) {
                        if (startPolling) {
                            pollFstepFiles(page);
                            startPolling = false;
                        }

                        self.params[page].pagingData._links = document._links;
                        self.params[page].pagingData.page = document.page;

                        deferred.resolve(document._embedded.fstepFiles);
                    }, function (error) {
                        MessageService.addError('Could not get Files', error);
                        deferred.reject();
                    });
            return deferred.promise;
        };

        this.removeFstepFile = function(file) {
            return $q(function(resolve, reject) {
                deleteAPI.from(rootUri + '/fstepFiles/' + file.id)
                         .newRequest()
                         .delete()
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('File removed', 'File ' + file.name + ' deleted.');
                        resolve(file);
                    } else {
                        MessageService.addError('Could not remove File ' + file.name, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not remove File ' + file.name, error);
                    reject();
                });
            });
        };

        this.uploadFile = function (page, newReference) {
            self.params[page].uploadStatus = "pending";
            self.params[page].uploadMessage = undefined;
            var deferred = $q.defer();
            var file = newReference.file;
            if (!file.$error) {

                Upload.upload({
                    url: fstepProperties.URLv2 + '/fstepFiles/refData',
                    data: {
                        file: file,
                        fileType: newReference.fileType,
                        userProperties: Upload.jsonBlob(newReference.userProperties)
                    }
                }).then(function (resp) {
                    MessageService.addInfo('File uploaded', 'Success ' + resp.config.data.file.name + ' uploaded.');
                    self.params[page].uploadStatus = "complete";
                    if (!resp.data.statusMessage || resp.data.statusMessage === 'OK') {
                        self.params[page].uploadMessage = "resp.config.data.file.name uploaded successfully";
                    } else {
                        self.params[page].uploadStatus = "warning";
                        self.params[page].uploadMessage = resp.data.statusMessage;
                    }
                    deferred.resolve(resp);
                }, function (resp) {
                    MessageService.addError('Error uploading File', resp.data);
                    self.params[page].uploadStatus = "failed";
                    self.params[page].uploadMessage = resp.data ? resp.data : "An undefined error occured";
                    deferred.reject();
                }, function (evt) {
                    self.params[page].progressPercentage = parseInt(100.0 * evt.loaded / evt.total);
                });
            }
            return deferred.promise;
        };

        // For search items we have to create a respective file first
        this.createGeoResultFile = function(item){

            var newProductFile = {
                properties: {
                    productSource: item.properties.productSource,
                    productIdentifier: item.properties.productIdentifier,
                    originalUrl: item.properties._links.fstep.href,
                    extraParams: item.properties.extraParams
                },
                type: item.type,
                geometry: item.geometry
            };

            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/fstepFiles/externalProduct')
                         .newRequest()
                         .post(newProductFile)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        resolve(JSON.parse(document.data));
                    } else {
                        reject();
                    }
                }, function (error) {
                    reject();
                });
            });
        };

        this.updateFstepFile = function (file) {
            var newfile = {name: file.filename, description: file.description, geometry: file.geometry, tags: file.tags};
            return $q(function(resolve, reject) {
                halAPI.from(rootUri + '/fstepFiles/' + file.id)
                         .newRequest()
                         .patch(newfile)
                         .result
                         .then(
                function (document) {
                    if (200 <= document.status && document.status < 300) {
                        MessageService.addInfo('File successfully updated', 'File ' + file.filename + ' has been updated.');
                        resolve(document);
                    } else {
                        MessageService.addError('Could not update File ' + file.filename, document);
                        reject();
                    }
                }, function (error) {
                    MessageService.addError('Could not update File ' + file.filename, error);
                    reject();
                });
            });
        };

        this.getFile = function (file) {
            var deferred = $q.defer();
            halAPI.from(rootUri + '/fstepFiles/' + file.id + "?projection=detailedFstepFile")
                     .newRequest()
                     .getResource()
                     .result
                     .then(
            function (document) {
                deferred.resolve(document);
            }, function (error) {
                MessageService.addError('Could not get File ' + file.filename, error);
                deferred.reject();
            });
            return deferred.promise;
        };

        /* Fetch a new page */
        this.getFstepFilesPage = function(page, url){
            if (self.params[page]) {
                self.params[page].pollingUrl = url;

                /* Get files list */
                self.getFstepFiles(page, url).then(function (data) {
                    self.params[page].files = data;
                });

                //the selected file will not exist on the new page
                self.params[page].selectedFile = undefined;
                self.params[page].fileDetails = undefined;
            }
        };

        this.getFstepFilesByFilter = function (page) {

            var params = self.params[page];

            if (params) {

                var url = rootUri + '/fstepFiles/search/parametricFind?sort=filename';

                if (params.selectedOwnershipFilter === self.fileOwnershipFilters.MY_FILES) {
                    url += '&owner=' + UserService.params.activeUser._links.self.href;
                } else if (params.selectedOwnershipFilter === self.fileOwnershipFilters.SHARED_FILES) {
                    url += '&notOwner=' + UserService.params.activeUser._links.self.href;
                }

                url += '&type='  + params.activeFileType;

                if (params.activeFileType === 'OUTPUT_PRODUCT') {
                    if (params.collection) {
                        url += '&collection=' + rootUri + '/collections/' + params.collection.id;
                    }
                    if (params.job) {
                        url += '&job=' + rootUri + '/jobs/' + params.job;
                    }
                }

                if (params.searchText) {
                    url += '&filter=' + params.searchText;
                }

                params.pollingUrl = url;

                params.files = [];
                return self.getFstepFiles(page).then(function(data) {
                    params.files = data;
                    return data;
                })

            }
        };

        this.refreshFstepFiles = function (page, action, file) {
            if(self.params[page]){

                if (action === "Remove" && self.params[page].selectedFile) {
                    if (file && file.id === self.params[page].selectedFile.id) {
                        self.params[page].selectedFile = undefined;
                        self.params[page].fileDetails = undefined;
                    }
                }


                self.getFstepFilesByFilter(page);
                self.refreshSelectedFstepFile(page);
            }
        };

        this.refreshSelectedFstepFile = function (page) {

            if (self.params[page]) {
                /* Get file contents if selected */
                if (self.params[page].selectedFile) {
                    self.getFile(self.params[page].selectedFile).then(function (file) {
                        self.params[page].fileDetails = file;
                        CommunityService.getObjectGroups(file, 'fstepFile').then(function (data) {
                            self.params[page].sharedGroups = data;
                        });
                    });
                }
            }

        };

        function estimateDownloadCost(file, $event) {
            EstimateCostService.estimateFileDownload(file).then(function(result) {
                EstimateCostService.showDownloadDialog($event, file, result);
            }, function(error) {
                EstimateCostService.showCostDialog($event, error);
            });
        }

        this.downloadFile = function($event, file){

            if(typeof file === "string") {
                var tempfile = {};
                tempfile.id = file.substr(file.lastIndexOf('/') + 1);
                this.getFile(tempfile).then(function(result){
                    estimateDownloadCost(result, $event);
                });
            } else {
                estimateDownloadCost(file, $event);
            }
        };


    return this;
  }]);
});
