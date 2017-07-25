'use strict';

// Make sure there are no obvious mistakes
module.exports = {
    options: {
        jshintrc: '.jshintrc',
        reporter: require('jshint-stylish')
    },
    all: {
        src: [
            '<%= fstep.app %>/{,*/}*.js',
            '<%= fstep.app %>/scripts/controllers/**/*.js',
            '<%= fstep.app %>/scripts/services/**/*.js'
        ]
    },
    test: {
        options: {
            jshintrc: 'test/.jshintrc'
        },
        src: ['test/spec/{,*/}*.js']
    }
};
