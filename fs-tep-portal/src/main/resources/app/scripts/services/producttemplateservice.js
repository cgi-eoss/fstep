/**
 * @ngdoc service
 * @name fstepApp.ProductService
 * @description
 * # ProducTemplatetService
 * Service in the fstepApp.
 */
'use strict';

define(['../fstepmodules', 'traversonHal'], function (fstepmodules, TraversonJsonHalAdapter) {


    fstepmodules.service('ProductTemplateService', ['$rootScope', 'CommunityService', 'UserService', 'MessageService', '$http', 'fstepProperties', '$q', 'traverson', '$timeout', function ( $rootScope, CommunityService, UserService, MessageService, $http, fstepProperties, $q, traverson, $timeout) {

        var self = this;
        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = fstepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();
        var deleteAPI = traverson.from(rootUri).useAngularHttp();

        this.templateOwnershipFilters = {
            MY_SERVICES: { id: 1, name: 'Mine', searchUrl: 'search/findByFilterAndOwner' },
            SHARED_SERVICES: { id: 2, name: 'Shared', searchUrl: 'search/findByFilterAndNotOwner' },
            ALL_SERVICES: { id: 0, name: 'All', searchUrl: 'search/findByFilterOnly'}
        };

        this.templateTypeFilters = {
            ALL_SERVICES: { id: 0, name: 'All Template Types' },
            APPLICATION: { id: 1, name: 'Application Template', value: 'APPLICATION' },
            PROCESSOR: { id: 2, name: 'Processor Template', value: 'PROCESSOR' },
            PARALLEL_PROCESSOR: { id: 3, name: 'Parallel Processor Template', value: 'PARALLEL_PROCESSOR' }
        };

        this.templatePublicationFilters = {
            ALL_SERVICES: { id: 0, name: 'All Publication Statuses' },
            PUBLIC_SERVICES: { id: 1, name: 'Public', value: 'PUBLIC_SERVICES'},
            PENDING_SERVICES: { id: 2, name: 'Pending', value: 'PENDING_SERVICES'},
            PRIVATE_SERVICES: { id: 3, name: 'Private', value: 'PRIVATE_SERVICES'}
        };

        let baseUrl = rootUri + '/serviceTemplates'


        var parseServicesResponse = function(document) {
            return {
                data: document._embedded.serviceTemplates,
                paging: document.page,
                links: document._links
            };
        }

        this.getServiceTemplates = function(params) {

            let url = baseUrl + '/search';
            if (params.owner === this.templateOwnershipFilters.MY_SERVICES.id) {
                url += '/findByFilterAndOwner';
            } else if (params.owner === this.templateOwnershipFilters.SHARED_SERVICES.id) {
                url += '/findByFilterAndNotOwner';
            } else {
                url += '/findByFilterOnly'
            }

            url += '?sort=type,name&owner=' + UserService.params.activeUser._links.self.href

            if (params.serviceType) {
                url += '&serviceType=' + params.serviceType
            }

            if (params.searchText) {
                url += '&filter=' + params.searchText;
            }

            return halAPI.from(url)
            .newRequest()
            .getResource()
            .result
            .then(function (document) {
                return parseServicesResponse(document);

            }, function (error) {
                MessageService.addError('Could not poll service templates', error);
            });
        }

        this.getServicesForType = function(serviceType) {

            let url = rootUri + '/services/search/findByFilterOnly';

            url += '?sort=name&size=100&serviceType=' + serviceType

            return halAPI.from(url)
            .newRequest()
            .getResource()
            .result
            .then(function (document) {
                return {
                    data: document._embedded.services
                }
            }, function (error) {
                MessageService.addError('Could not get services', error);
            });
        }

        this.getDefaultTemplate = function(serviceType) {
            return halAPI.from(baseUrl + '/search/getDefaultByType?serviceType=' + serviceType)
            .newRequest()
            .getResource()
            .result
            .then(function (document) {
                return document;

            }, function (error) {
                MessageService.addError('Could not get default template for service type', error);
            });
        }

        this.getTemplateDetails = function(template) {
            var deferred = $q.defer();
            halAPI.from(baseUrl + '/' + template.id + '?projection=detailedFstepServiceTemplate')
                       .newRequest()
                       .getResource()
                       .result
                       .then(
            function (document) {

                deferred.resolve(document);
            }, function (error) {
                MessageService.addError('Could not get details for template ' + template.name, error);
                deferred.reject();
            });
            return deferred.promise;
        };


        this.getTemplateFiles = function(template) {
            var deferred = $q.defer();
            var request = halAPI.from(rootUri + '/serviceTemplateFiles/search/findByServiceTemplate')
                .newRequest()
                .withRequestOptions({
                    qs: { serviceTemplate: template._links.self.href }
                })
                .getResource()
                .result
                .then(
                    function (document) {
                        deferred.resolve(document._embedded.serviceTemplateFiles);
                    }, function (error) {
                        MessageService.addError('Could not get service template files', error);
                        deferred.reject();
                    }
                );

            return deferred.promise;
        }

        this.getFileDetails = function(file) {
            var deferred = $q.defer();
            halAPI.from(file._links.self.href)
                       .newRequest()
                       .getResource()
                       .result
                       .then(
                function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Could not get service template file details', error);
                    deferred.reject();
                }
            );
            return deferred.promise;
        }

        this.addFile = function(file){
            var deferred = $q.defer();
            halAPI.from(rootUri + '/serviceTemplateFiles/')
                     .newRequest()
                     .post(file)
                     .result
                     .then(
              function (result) {
                  MessageService.addInfo('Template service file added', file.filename + ' added');
                  deferred.resolve(JSON.parse(result.data));
              }, function (error) {
                  MessageService.addError('Could not add Service File ' + file.filename, error);
                  deferred.reject();
              });
            return deferred.promise;
        };


        this.removeServiceFile = function(file){
            return $q(function(resolve, reject) {
                deleteAPI.from(file._links.self.href)
                         .newRequest()
                         .delete()
                         .result
                         .then(
                    function (document) {
                        if (200 <= document.status && document.status < 300) {
                            MessageService.addInfo('Service File removed', 'File ' + file.filename + ' deleted.');
                            resolve();
                        } else {
                            MessageService.addError('Could not remove Service File ' + file.filename, document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not remove Service File ' + file.filename, error);
                        reject();
                    }
                );
            });
        };

        this.removeTemplate = function(template) {
            return $q(function(resolve, reject) {
                deleteAPI.from(baseUrl + '/' + template.id)
                         .newRequest()
                         .delete()
                         .result
                         .then(
                    function (document) {
                        if (200 <= document.status && document.status < 300) {
                            MessageService.addInfo('Template removed', 'Template ' + template.name + ' deleted.');
                            resolve(template);
                        } else {
                            MessageService.addError('Could not remove Template ' + template.name, document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not remove Template ' + template.name, error);
                        reject();
                    }
                );
            });
        }

        this.createService = function(data){
            return $q(function(resolve, reject) {
                  var service = {
                          name: data.name,
                          description: data.description,
                          dockerTag: 'fstep/' + data.name.toLowerCase(),
                          serviceDescriptor: {
                            description: data.description,
                            id: data.name,
                            title: data.title,
                            serviceProvider: data.name,
                            version: '0.1',
                            serviceType: 'Java'
                        }
                  };
                  halAPI.from(rootUri + '/serviceTemplates/' + data.serviceTemplate + '/newService')
                           .newRequest()
                           .post(service)
                           .result
                           .then(
                    function (document) {
                        if (200 <= document.status && document.status < 300) {
                            MessageService.addInfo('Service added', 'New Service ' + data.name + ' added.');
                            resolve(JSON.parse(document.data));
                        } else {
                            MessageService.addError('Could not create Service ' + data.name, document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not add Service ' + data.name, error);
                        reject();
                    }
                );
            });
        };

        this.createTemplateFromService = function(data){
            return $q(function(resolve, reject) {
                  var service = {
                          name: data.name,
                          description: data.description,
                          serviceDescriptor: {
                            description: data.description,
                            id: data.name,
                            title: data.title,
                            serviceProvider: data.name,
                            version: '0.1',
                            serviceType: 'Java'
                        }
                  };
                  halAPI.from(rootUri + '/services/' + data.fromService + '/newTemplate')
                           .newRequest()
                           .post(service)
                           .result
                           .then(
                    function (document) {
                        if (200 <= document.status && document.status < 300) {
                            MessageService.addInfo('Template added', 'New Template ' + data.name + ' added.');
                            resolve(JSON.parse(document.data));
                        } else {
                            MessageService.addError('Could not create Service ' + data.name, document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not add Service ' + data.name, error);
                        reject();
                    }
                );
            });
        };

        this.createTemplate = function(data){
            return $q(function(resolve, reject) {
                  var template = {
                          name: data.name,
                          description: data.description,
                          serviceDescriptor: {
                            description: data.description,
                            id: data.name,
                            title: data.title,
                            serviceProvider: data.name,
                            version: '0.1',
                            serviceType: 'Java'
                        }
                  };
                  halAPI.from(baseUrl + '/')
                           .newRequest()
                           .post(template)
                           .result
                           .then(
                    function (document) {
                        if (200 <= document.status && document.status < 300) {
                            MessageService.addInfo('Template added', 'New Template ' + data.name + ' added.');
                            resolve(JSON.parse(document.data));
                        } else {
                            MessageService.addError('Could not create Template ' + data.name, document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not add Template ' + data.name, error);
                        reject();
                    }
                );
            });
        };

        var updateFile = function(file) {
            var deferred = $q.defer();
            var editedFile = angular.copy(file);
            editedFile.content =  btoa(file.content);
            halAPI.from(file._links.self.href)
                       .newRequest()
                       .patch(editedFile)
                       .result
                       .then(
                function (result) {
                    deferred.resolve();
                }, function (error) {
                    MessageService.addError('Could not update Template File ' + file.name, error);
                    deferred.reject();
                }
            );
            return deferred.promise;
        }


        var saveFiles = function(template) {
            if(template.files) {
                var promises = [];
                for(var i = 0; i < template.files.length; i++){
                    var partialPromise = updateFile(template.files[i]);
                    promises.push(partialPromise);
                }
                $q.all(promises).then(function(){
                    MessageService.addInfo('Template updated', 'Template ' + template.name + ' successfully updated');
                });
            }
        }

        this.saveTemplate = function(template) {

            // Some descriptor fields are a copy from service itself
            if(!template.description) {
                template.description = '';
            }
            if(!template.serviceDescriptor) {
                template.serviceDescriptor = {};
            }
            template.serviceDescriptor.description = template.description;
            template.serviceDescriptor.id = template.name;
            template.serviceDescriptor.serviceProvider = template.name;

            var editService = {
                name: template.name,
                description: template.description,
                dockerTag: template.dockerTag,
                serviceDescriptor: template.serviceDescriptor,
                type: template.type
            };

            return $q(function(resolve, reject) {
                halAPI.from(baseUrl + '/' + template.id)
                           .newRequest()
                           .patch(editService)
                           .result
                           .then(
                    function (document) {
                        if (200 <= document.status && document.status < 300) {
                            if(template.files) {
                                saveFiles(template);
                            }
                            resolve(document);
                        } else {
                            MessageService.addError('Could not update Service ' + template.name, document);
                            reject();
                        }
                    }, function (error) {
                        MessageService.addError('Could not update Service ' + template.name, error);
                        reject();
                    }
                );
            });
        };

        return this;
    }]);
})

