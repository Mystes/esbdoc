'use strict';

/**
 * @ngdoc overview
 * @name esbdocPocApp
 * @description
 * # esbdocPocApp
 *
 * Main module of the application.
 */
angular
  .module('esbdocPocApp', [
    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ngRoute',
    'ngSanitize',
    'ngTouch'
  ]).config(function ($routeProvider) {
    $routeProvider
      .when('/', {
        controller: 'MainCtrl'
      })
      .otherwise({
        redirectTo: '/'
      });
  });
