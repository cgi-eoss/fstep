'use strict';

// Empties folders to start fresh
module.exports = {
    options: {
    },
    dist: {
      src: ['.tmp/post/styles/**/*.css'],
      dest: '<%= fstep.app %>/main.css',
    }
};
