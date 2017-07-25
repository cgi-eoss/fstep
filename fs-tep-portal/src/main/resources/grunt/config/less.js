 'use strict';

// Watches less files and coverts them to css
 module.exports = {
     options: {
         paths: ["<%= fstep.app %>/styles/less"],
         yuicompress: true
     },
     files: {
         expand: true,
         cwd: '<%= fstep.app %>/styles/less',
         src: ['**/*.less', '!{variable, mixins}*.less'],
         dest: '.tmp/styles',
         ext: '.css'
     },
 };
