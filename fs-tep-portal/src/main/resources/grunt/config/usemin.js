'use strict';

// Performs rewrites based on filerev and the useminPrepare configuration
module.exports = {
    html: ['<%= fstep.dist %>/{,*/}*.html'],
    css: ['<%= fstep.dist %>/styles/{,*/}*.css'],
    js: ['<%= fstep.dist %>/scripts/{,*/}*.js'],
    options: {
        assetsDirs: [
          '<%= fstep.dist %>',
          '<%= fstep.dist %>/images',
          '<%= fstep.dist %>/styles'
        ],
        patterns: {
            js: [[/(images\/[^''""]*\.(png|jpg|jpeg|gif|webp|svg))/g, 'Replacing references to images']]
        }
    }
};
