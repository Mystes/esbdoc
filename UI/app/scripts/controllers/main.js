'use strict';


/**
 * @ngdoc function
 * @name esbdocPocApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the esbdocPocApp
 */
angular.module('esbdocPocApp')
  .controller('MainCtrl', function ($scope, $timeout) {

    $scope.selected = {resource: null, showDependencyMap: true};

    if(window.ESBDOCDATA) {
      $scope.esbDoc = window.ESBDOCDATA;
      angular.forEach($scope.esbDoc.resources, function (value, key) {
        value.humanizedName = key.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/_/g, ' ');
      });
    } else {
      return;
    }

    var countObjectKeys = function(obj) {
      var keys = 0;
      for(var k in obj) keys++;
      return keys;
    };

    var calculateTestStatistics = function() {
      var notReached = new Array();
      var processedTests = new Array();
      var tests = $scope.esbDoc.tests;
      var resources = $scope.esbDoc.resources;
      var amountOfTests = 0;
      var amountOfHappyTests = 0;
      var amountOfSadTests = 0;
      var resourceAmount = Object.keys($scope.esbDoc.resources).length;
      for(var k in resources) {
        if(resources[k].type != 'task') {
            if(tests[k]) {
                for (var projectKey in tests[k]) {
                    var project = tests[k][projectKey];
                    for (var suiteKey in project.suites) {
                        var suite = project.suites[suiteKey];
                        for (var testCaseKey in suite.cases) {
                            var testCase = suite.cases[testCaseKey];
                            if($.inArray(testCase.name, processedTests) == -1) {
                                processedTests.push(testCase.name);
                                amountOfTests++;
                                if (testCase.name.trim().indexOf("Happy") === 0) {
                                    amountOfHappyTests++;
                                } else if(testCase.name.trim().indexOf("Sad") === 0) {
                                    amountOfSadTests++;
                                }
                            }
                        }
                    }
                }
            } else {
                notReached.push(k);
            }
        }
      }
      /* set statistics to scope */

      $scope.amountOfTests = amountOfTests;
      $scope.amountOfHappyTests = amountOfHappyTests;
      $scope.amountOfSadTests = amountOfSadTests;
      $scope.resourcesNotReached = notReached;
      $scope.testCoverage = (((resourceAmount - notReached.length) / resourceAmount) * 100).toFixed(0);
    };

    $scope.humanizedNameFunc = function(resource)
    {
      return resource.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/_/g, ' ');
    }

    var resourcesNotReachedByTests = function() {
      var notReached = new Array();
      var tests = $scope.esbDoc.tests;
      var resources = $scope.esbDoc.resources;
      for(var k in resources) {
        if(!tests[k]) {
            notReached.push(k);
        }
      }
      return notReached;
    };

    var resourcesWithNoForwardOrReverseDependencies = function() {
      var selectedResources = new Array();
      var forward = $scope.esbDoc.dependencies.forward;
      var reverse = $scope.esbDoc.dependencies.reverse;
      var tests = $scope.esbDoc.tests;
      var resources = $scope.esbDoc.resources;
      for(var k in resources) {
        if(resources[k].type != 'task') {
            if((!forward[k] || forward[k].length == 0) && (!reverse[k] || reverse[k].length == 0) && !tests[k]) {
                selectedResources.push(k);
            }
        }
      }
      return selectedResources;
    };

    var hasESBDoc = function(obj) {
        if (!obj) {
            return false;
        }
        if (obj.purpose || obj.receives || obj.returns || obj.dependencies) {
            return true;
        }
        return false;
    }

    var esbDocCoverage = function() {
      var selectedResources = new Array();
      var esbDocCount = 0;
      var resources = $scope.esbDoc.resources;
      var resourceAmount = 0;
      for(var k in resources) {
        if(resources[k].type === 'proxy' || resources[k].type === 'sequence' || resources[k].type === 'api') {
            resourceAmount++;
            if(hasESBDoc(resources[k])) {
                esbDocCount++;
            } else {
                selectedResources.push(k);
            }
        }
      }
      $scope.resourcesMissingDescription = selectedResources;
      return ((esbDocCount / resourceAmount) * 100).toFixed(0);
    };

    var keyResources = function() {
      var keyResources = {};
      var reverse = $scope.esbDoc.dependencies.reverse;
      var resources = $scope.esbDoc.resources;
      for(var k in resources) {
        if((!reverse[k] || reverse[k].length == 0) && (resources[k].type === 'proxy' || resources[k].type === 'api')) {
            keyResources[k] = resources[k];
        }
      }
      return keyResources;
    }

    /* Statistics calculations */
    $scope.numberOfResources = countObjectKeys($scope.esbDoc.resources);
    $scope.numberOfDependencies = countObjectKeys($scope.esbDoc.dependencies.forward) + ' forward and ' + countObjectKeys($scope.esbDoc.dependencies.reverse) + ' reverse';
    calculateTestStatistics();
    $scope.esbDocCoverage = esbDocCoverage();
    $scope.resourcesWithNoForwardOrReverseDependencies = resourcesWithNoForwardOrReverseDependencies();
    $scope.keyResources = keyResources();

    $scope.searchResourcesFilter = function(items, search) {
        var result = {};
        angular.forEach(items, function(value, key) {
            if (key.toUpperCase().indexOf(search.toUpperCase()) >= 0) {
                result[key] = value;
            }
        });
        return result;
    }

    $scope.awesomeThings = [
      'HTML5 Boilerplate',
      'AngularJS',
      'Karma'
    ];
  })
  .directive('dependencyMap',['$timeout', function($timeout) {
    var nodeTypeStyles = {
      'proxy': 'stroke: #00f; fill: #eef',
      'sequence': 'stroke: #ccc; fill: #eee;',
      'endpoint': 'stroke: #0c0; fill: #efe',
      'api': 'stroke: #44ff66;',
      'messageProcessor': 'stroke: #44ff66;',
      'messageStore': 'stroke: #44ff66;',
      'dataservice': 'stroke: #44ff66;'
    };

    var edgeTypeStyles = {
      'sequence': 'stroke: #ccc;',
      'inSequence': 'stroke: #039;',
      'outSequence': 'stroke: #039;',
      'faultSequence': 'stroke: #ff0000;',
      'onErrorSequence': 'stroke: #ff0000;',
      'proxyEndpoint': '#039; stroke-dasharray: 0,1 1;',
      'clone': 'stroke: #666; stroke-dasharray: 0,2 1;',
      'iterate': 'stroke: #ccc; stroke-dasharray: 0,2 1;',
      'send': 'stroke: #039; stroke-dasharray: 0,1 1;',
      'call': 'stroke: #039; stroke-dasharray: 0,1 1;',
      'callout': 'stroke: #039; stroke-dasharray: 0,1 1;',
      'endpoint': 'stroke: #039; stroke-dasharray: 0,1 1;',
      'documented dependency': 'stroke: #ffff66;'
    };

      function link(scope, element, attrs) {
        scope.$watch(attrs.resources, function(value) {
            if(!attrs.resources)
                return

            angular.element("#dagre").empty();

            var g = new dagreD3.Digraph();
            var layout = dagreD3.layout()
                  .nodeSep(50)
                  .edgeSep(10)
            //                          .rankSep(1)
                  .rankDir("LR");

            g.addNode(scope.selected.resource,    { label: scope.esbDoc.resources[scope.selected.resource].humanizedName });
            g.node(scope.selected.resource).style = 'fill: #9cf';

            var findPath = function(startingPoint, stepsRemaining, backward) {
                if(stepsRemaining < 0)
                    return;

                stepsRemaining --;

                var data = backward ? scope.esbDoc.dependencies.reverse[startingPoint] : scope.esbDoc.dependencies.forward[startingPoint];

                angular.forEach(data, function (value, k) {
                    var key = backward ? value.source : value.target;
                    if(!g.hasNode(key)) {
                        g.addNode(key, { label: scope.esbDoc.resources[key].humanizedName });
                        if(scope.esbDoc.resources[key] && nodeTypeStyles[scope.esbDoc.resources[key].type]) {
                            //g.node(key).style = nodeTypeStyles[$scope.esbDoc.resources[key].type];
                            g.node(key).class = scope.esbDoc.resources[key].type;
                        }
                    }

                    if(backward) {
                        var id = key+'--to--'+startingPoint+'--via--'+value.type;
                        if(!g.hasEdge(id)) {
                            g.addEdge(id, key, startingPoint, { label: (value.type === 'sequence' ? '' : value.type) });
                        }
                        if(edgeTypeStyles[value.type]) {
                            g.edge(id).style = edgeTypeStyles[value.type];
                        }
                    } else {
                        var id = startingPoint+'--to--'+key+'--via--'+value.type;
                        if(!g.hasEdge(id)) {
                            g.addEdge(id, startingPoint, key, { label: (value.type === 'sequence' ? '' : value.type) });
                        }
                        if(edgeTypeStyles[value.type]) {
                            g.edge(id).style = edgeTypeStyles[value.type];
                        }
                    }

                    findPath(key, stepsRemaining, backward);
                });

            };

            findPath(scope.selected.resource, 30, true);
            findPath(scope.selected.resource, 30, false);

            var renderer = new dagreD3.Renderer();

            // Set up an SVG group so that we can translate the final graph.
            var svg = d3.select('svg'),
            centerG = svg.append('g'),
            zoomG = centerG.append('g');

            // Set initial zoom to 75%.
            var initialScale = 1;
            var zoom = dagreD3.zoom.panAndZoom(zoomG);
            dagreD3.zoom(svg, zoom);
            // We must set the zoom and then trigger the zoom event to synchronize D3 and
            // the DOM.
            zoom.scale(initialScale).event(svg);


            var oldDrawNodes = renderer.drawNodes();
            renderer.drawNodes(function(g, svg) {
                var svgNodes = oldDrawNodes(g, svg);

                // Set the title on each of the nodes and use tipsy to display the tooltip on hover
                svgNodes.attr('class', function(d) { return g.node(d).class;})
                .attr('onClick', function(d) { return 'selected.resource = "' + d + '"';})
                .each(function(d) {
                    angular.element(this).on('click', function(e) {
                        angular.element(e.target).scope().selected.resource = d;
                        angular.element(e.target).scope().$apply();
                    });
                });

                return svgNodes;
            });


            var layout2 = renderer.layout(layout).run(g, zoomG);

            // Center graph on viewport
            $timeout(function() {
                if(svg) {
                    var scalex = parseInt(svg.style("width")) / layout2.graph().width;
                    var scaley = parseInt(svg.style("height")) / layout2.graph().height;
                    var scale = scaley;

                    if(scalex < scaley) {
                        scale = scalex;
                    }
                    if(scale > 1) {
                        scale = 1;
                    }

                    zoomG.attr('transform', 'translate(' + layout2.graph().width / 2 + ',' + layout2.graph().height / 2 + ')');
                    zoom.scale(scale).event(svg);
                }
            },100, true);
        });
      }

      return {
        link: link
      };
    }])
  .directive('prettyPrintCode', function() {

    function link(scope, element, attrs) {
        scope.$watch(attrs.code, function(value) {
            if (value) {
                var code = value;
                var pretty;
                if (code.trim().indexOf("<") === 0) {
                    pretty = vkbeautify.xml(code);
                } else if (code.trim().indexOf("{") === 0) {
                    pretty = vkbeautify.json(code);
                } else {
                    pretty = code;
                }
                element.text(pretty);

                /* Remove pretty print classes that marks element already processed */
                element.removeClass('prettyprinted');
                /* Mark it to be prettyprinted */
                element.addClass('prettyprint');
                prettyPrint();
            }
        });
    }

    return {
      link: link
    };
  });

