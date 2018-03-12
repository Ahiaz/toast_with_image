# ABOUT THIS PLUGIN:

I was develop this plugin for a requirement that inappbrowser doesnt supply it (loading an image when navigate throught the pages)

# INSTALL FROM CMD: cordova plugin add https://github.com/Ahiaz/toast_with_image

This plugin is a modified version of https://github.com/EddyVerbruggen/Toast-PhoneGap-Plugin
I added a function to show an image in screen with a blinking effect and the another functions remain like the original plugin.

# USAGE:

First of all, you need to install ng-cordova plugin to works properly if it will used on ionic v1:
https://github.com/ionic-team/ng-cordova download in your lib folder. Add to index: <script src="js/ng-cordova-master/dist/ng-cordova.js"></script> and inject to app.js: var app = angular.module('app', ['ionic','ngCordova']); and inject toast dependency into controller: app.controller('settingCtrl', function($rootScope, $scope, $q, $location, $state, $timeout, $ionicPlatform, $cordovaToast)

# FUNCTION:
Show an image from url, locally or online. Supported by picasso library, this allow jpeg, png and gif images.

* window.plugins.toast.showWithImage("https://cdn.appstorm.net/mac.appstorm.net/files/2012/07/icon4.png","CENTER",8000,3000,"url",30);

# PARAMETERS:

* **URL**: image hosting.
* **POSITION**: CENTER, BOTTOM, TOP (all center in screen).
* **DURATION**: Toast duration.
* **BLINKING EFFECT DURATION**: experimental property, you can adjust and test the values in your aplication.
* **FROM**: Url or resource(the resource option only works if you clone this repository and change the res static images and create your own rep).
* **PERCENTAGE**: % of screen that image will occupy to make it responsive to all devices.

if you need support: jahiaz@gmail.com

**THANKS TO EDDY FOR THE AWESOME TOAST PLUGIN**.






