define([
    //contollers
    './controllers/index',
    './controllers/navbar',

    './controllers/admin/admin',

    './controllers/account/account',

    './controllers/common/messages',
    './controllers/common/bottombar/bottombar',
    './controllers/common/bottombar/databasket',
    './controllers/common/bottombar/jobs',
    './controllers/common/bottombar/results',
    './controllers/common/directives/imageLoader',
    './controllers/common/directives/aoiField',
    './controllers/common/directives/aoiList',
    './controllers/common/directives/dateRangeField',
    './controllers/common/directives/dynamicForm',

    './controllers/community/community',
    './controllers/community/sidebar/databaskets',
    './controllers/community/sidebar/files',
    './controllers/community/sidebar/groups',
    './controllers/community/sidebar/jobs',
    './controllers/community/sidebar/systematicprocessings',
    './controllers/community/sidebar/projects',
    './controllers/community/sidebar/collections',
    './controllers/community/sidebar/services',
    './controllers/community/sidebar/servicetemplates',
    './controllers/community/manage/databasket',
    './controllers/community/manage/file',
    './controllers/community/manage/group',
    './controllers/community/manage/job',
    './controllers/community/manage/systematicprocessing',
    './controllers/community/manage/project',
    './controllers/community/manage/collection',
    './controllers/community/manage/service',
    './controllers/community/manage/servicetemplate',

    './controllers/developer/developer',
    './controllers/developer/manage/service',
    './controllers/developer/sidebar/services',
    './controllers/developer/manage/template',
    './controllers/developer/sidebar/templates',

    './controllers/explorer/explorer',
    './controllers/explorer/map',
    './controllers/explorer/sidebar/project',
    './controllers/explorer/sidebar/search',
    './controllers/explorer/sidebar/services',
    './controllers/explorer/sidebar/workspace',

    './controllers/userendpoint/userendpoint',

    './controllers/helpdesk/helpdesk',

    //services
    './services/apikeyservice',
    './services/basketservice',
    './services/commonservice',
    './services/communityservice',
    './services/fileservice',
    './services/groupservice',
    './services/jobservice',
    './services/messageservice',
    './services/collectionservice',
    './services/projectservice',
    './services/productservice',
    './services/producttemplateservice',
    './services/publishingservice',
    './services/searchservice',
    './services/systematicservice',
    './services/tabservice',
    './services/userservice',
    './services/walletservice',
    './services/userprefservice',
    './services/usermountsservice',
    './services/userendpointservice',
    './services/aoiservice',
    './services/map/mapservice',
    './services/map/aoilayerservice',
    './services/map/resultlayerservice',
    './services/map/basketlayerservice',
    './services/map/productlayerservice'


], function () {});
