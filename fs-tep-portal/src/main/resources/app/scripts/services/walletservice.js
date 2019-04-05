/**
 * @ngdoc service
 * @name fstepApp.WalletService
 * @description
 * # WalletService
 * Service for the user's wallet to keep track of used coins.
 */
'use strict';

define(['../fstepmodules', 'traversonHal', 'moment'], function (fstepmodules, TraversonJsonHalAdapter, moment) {

    fstepmodules.service('WalletService', [ 'fstepProperties', '$q', 'traverson', 'MessageService', function (fstepProperties, $q, traverson, MessageService) {

        var self = this;

        traverson.registerMediaType(TraversonJsonHalAdapter.mediaType, TraversonJsonHalAdapter);
        var rootUri = fstepProperties.URLv2;
        var halAPI =  traverson.from(rootUri).jsonHal().useAngularHttp();

        var userWallet;

        this.transactionTypes = [{
            name: 'CREDIT',
            title: 'Credit'
        }, {
            name: 'JOB_PROCESSING',
            title: 'Processing job'
        }, {
            name: 'DOWNLOAD',
            title: 'Download'
        }];

        this.transactionTypesMap = this.transactionTypes.reduce(function(typesMap, type) {
            typesMap[type.name] = type.title;
            return typesMap;
        }, {})

        this.transactionTypesMap['JOB'] = 'Processing job';

        this.params = {
            account: {
                ledger: {
                    pagingData: {},
                    transactions: undefined,
                    transactionTypes: this.transactionTypes,
                    filters: {
                        type: undefined
                    }
                },
                wallet: undefined
            }
        };

        this.getUserWallet = function(user){
            var deferred = $q.defer();
            userWallet = halAPI.from(rootUri + '/users/' + user.id)
                .newRequest()
                .follow('wallet')
                .getResource();

            userWallet.result
                .then(function (document) {
                    deferred.resolve(document);
                }, function (error) {
                    MessageService.addError('Failed to get Wallet', error);
                    deferred.reject();
                }
            );
            return deferred.promise;
        };

        this.getTransactions = function(page, user, url) {

            if (self.params[page]) {

                let queryData = self.params[page].ledger
                if (!url) {
                    url = rootUri + '/walletTransactions/search/parametricFind'  +
                    '?sort=transactionTime,desc';

                    if (user) {
                        url += '&owner=' + user
                    }

                    if (queryData.filters.type) {
                        url += '&type=' + queryData.filters.type;
                    }
                    if (queryData.filters.dateRange && queryData.filters.dateRange.enabled) {
                        if (queryData.filters.dateRange.start) {
                            url += '&startDateTime=' + moment(queryData.filters.dateRange.start).format('YYYY-MM-DD[T00:00:00Z]');
                        }
                        if (queryData.filters.dateRange.end) {
                            url += '&endDateTime=' + moment(queryData.filters.dateRange.end).format('YYYY-MM-DD[T23:59:59Z]');
                        }
                    }
                }

                var deferred = $q.defer();
                halAPI.from(url)
                    .newRequest()
                    .getResource()
                    .result
                    .then(function(document) {
                        queryData.pagingData._links = document._links;
                        queryData.pagingData.page = document.page;

                        queryData.transactions = document._embedded.walletTransactions;
                        deferred.resolve(queryData.transactions);
                    }, function(error) {
                        MessageService.addError('Could not get wallet transactions', error);
                        deferred.reject();
                    });

                return deferred.promise;
            }
        };

        this.getTransactionResource = function(transaction) {
            var deferred = $q.defer();
            halAPI.from(transaction._links.associated.href)
                .newRequest()
                .getResource()
                .result
                .then(function(document) {
                    deferred.resolve(document);
                }, function(error) {
                    MessageService.addError('Could not get wallet transaction', error);
                    deferred.reject();
                });

            return deferred.promise;
        }


        this.makeTransaction = function(user, wallet, coins){
            return $q(function(resolve, reject) {
                var credit = {amount: coins};
                traverson.from(rootUri).json().useAngularHttp().from(wallet._links.self.href + '/credit')
                         .newRequest()
                         .post(credit)
                         .result
                         .then(
                function (document) {
                    MessageService.addInfo('User Coin Balance updated', coins + ' coins added to user '.concat(user.name));
                    resolve();
                }, function (error) {
                    MessageService.addError('Failed to update Coin Balance', error);
                    reject();
                });
            });
        };

        return this;
    }]);
});
