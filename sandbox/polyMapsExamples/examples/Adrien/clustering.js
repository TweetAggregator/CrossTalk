//var po = org.polymaps;

//var map = po.map()
    //.container(document.getElementById("map").appendChild(po.svg("svg")))
    map.center({lat: 37.787, lon: -122.228})
    .zoom(3)
    .zoomRange([3, 10]);
map.on("move", update2);

//The variables for the display
  //TODO this has to be furnished by scala !
  //Maybe even 3 different lists actually ... nope ?
  //We expect {"clusters": [{"centers": [{"x":_, "y":_, "r":_}]}]} where all vars are in geoCoordinates

//var rawData = JSON.parse('{"clusters": ["centers": [{"x": -89.49999999999997,"y": 39.12499999999999, "r": 40.39999999999999, "d": 100.0},{"x": -110.5,"y": 46.774999999999984, "r": 48.04999999999998, "d": 100.0},{"x": -110.5,"y": 44.22499999999999, "r": 45.499999999999986, "d": 59.0},{"x": -81.09999999999997,"y": 36.574999999999996, "r": 37.849999999999994, "d": 88.0},{"x": -102.1,"y": 23.825000000000003, "r": 25.1, "d": 100.0},{"x": -102.1,"y": 49.32499999999998, "r": 50.59999999999998, "d": 7.0},{"x": -114.69999999999999,"y": 49.32499999999998, "r": 50.59999999999998, "d": 12.0},{"x": -102.1,"y": 41.67499999999999, "r": 42.94999999999999, "d": 10.0},{"x": -85.29999999999998,"y": 41.67499999999999, "r": 42.94999999999999, "d": 100.0},{"x": -81.09999999999997,"y": 44.22499999999999, "r": 45.499999999999986, "d": 100.0},{"x": -85.29999999999998,"y": 21.275, "r": 22.55, "d": 100.0},{"x": -123.1,"y": 36.574999999999996, "r": 37.849999999999994, "d": 100.0},{"x": -102.1,"y": 46.774999999999984, "r": 48.04999999999998, "d": 56.0},{"x": -106.29999999999998,"y": 34.025, "r": 35.3, "d": 82.0},{"x": -97.89999999999998,"y": 36.574999999999996, "r": 37.849999999999994, "d": 70.0},{"x": -93.69999999999999,"y": 41.67499999999999, "r": 42.94999999999999, "d": 100.0},{"x": -97.89999999999998,"y": 23.825000000000003, "r": 25.1, "d": 50.0},{"x": -81.09999999999997,"y": 31.475, "r": 32.75, "d": 100.0},{"x": -102.1,"y": 26.375, "r": 27.650000000000002, "d": 100.0},{"x": -102.1,"y": 44.22499999999999, "r": 45.499999999999986, "d": 23.0},{"x": -114.69999999999999,"y": 46.774999999999984, "r": 48.04999999999998, "d": 13.0},{"x": -114.69999999999999,"y": 34.025, "r": 35.3, "d": 100.0},{"x": -110.5,"y": 34.025, "r": 35.3, "d": 100.0},{"x": -114.69999999999999,"y": 39.12499999999999, "r": 40.39999999999999, "d": 47.0},{"x": -97.89999999999998,"y": 26.375, "r": 27.650000000000002, "d": 100.0},{"x": -97.89999999999998,"y": 46.774999999999984, "r": 48.04999999999998, "d": 100.0},{"x": -93.69999999999999,"y": 46.774999999999984, "r": 48.04999999999998, "d": 100.0},{"x": -81.09999999999997,"y": 39.12499999999999, "r": 40.39999999999999, "d": 100.0},{"x": -123.1,"y": 34.025, "r": 35.3, "d": 30.0},{"x": -85.29999999999998,"y": 31.475, "r": 32.75, "d": 98.0},{"x": -85.29999999999998,"y": 49.32499999999998, "r": 50.59999999999998, "d": 100.0},{"x": -85.29999999999998,"y": 46.774999999999984, "r": 48.04999999999998, "d": 14.0},{"x": -127.30000000000001,"y": 49.32499999999998, "r": 50.59999999999998, "d": 9.0},{"x": -118.9,"y": 34.025, "r": 35.3, "d": 102.0},{"x": -85.29999999999998,"y": 34.025, "r": 35.3, "d": 99.0},{"x": -81.09999999999997,"y": 21.275, "r": 22.55, "d": 100.0},{"x": -97.89999999999998,"y": 28.925000000000004, "r": 30.200000000000003, "d": 92.0},{"x": -93.69999999999999,"y": 44.22499999999999, "r": 45.499999999999986, "d": 100.0}], "centers": [{"x": -127.30000000000001,"y": 44.22499999999999, "r": 50.59999999999998, "d": 1.8},{"x": -97.89999999999998,"y": 36.574999999999996, "r": 42.94999999999999, "d": 10.48},{"x": -108.39999999999999,"y": 27.650000000000002, "r": 30.200000000000003, "d": 9.65},{"x": -83.19999999999997,"y": 35.3, "r": 45.499999999999986, "d": 24.0625},{"x": -95.79999999999998,"y": 48.04999999999998, "r": 50.59999999999998, "d": 20.375},{"x": -104.19999999999999,"y": 21.275, "r": 22.55, "d": 8.583333}], "centers": [{"x": -127.30000000000001,"y": 44.22499999999999, "r": 50.59999999999998, "d": 1.8},{"x": -97.89999999999998,"y": 36.574999999999996, "r": 42.94999999999999, "d": 10.48},{"x": -108.39999999999999,"y": 27.650000000000002, "r": 30.200000000000003, "d": 9.65},{"x": -83.19999999999997,"y": 35.3, "r": 45.499999999999986, "d": 24.0625},{"x": -95.79999999999998,"y": 48.04999999999998, "r": 50.59999999999998, "d": 20.375},{"x": -104.19999999999999,"y": 21.275, "r": 22.55, "d": 8.583333}], "centers": [{"x": -127.30000000000001,"y": 44.22499999999999, "r": 50.59999999999998, "d": 1.8},{"x": -108.39999999999999,"y": 27.650000000000002, "r": 30.200000000000003, "d": 9.65},{"x": -97.89999999999998,"y": 40.39999999999999, "r": 42.94999999999999, "d": 11.0},{"x": -83.19999999999997,"y": 35.3, "r": 45.499999999999986, "d": 24.0625},{"x": -95.79999999999998,"y": 48.04999999999998, "r": 50.59999999999998, "d": 20.375},{"x": -104.19999999999999,"y": 21.275, "r": 22.55, "d": 8.583333},{"x": -110.5,"y": 35.3, "r": 37.849999999999994, "d": 21.333334}], "centers": [{"x": -127.30000000000001,"y": 44.22499999999999, "r": 50.59999999999998, "d": 1.8},{"x": -108.39999999999999,"y": 27.650000000000002, "r": 30.200000000000003, "d": 9.65},{"x": -97.89999999999998,"y": 40.39999999999999, "r": 42.94999999999999, "d": 11.0},{"x": -83.19999999999997,"y": 35.3, "r": 45.499999999999986, "d": 24.0625},{"x": -95.79999999999998,"y": 48.04999999999998, "r": 50.59999999999998, "d": 20.375},{"x": -104.19999999999999,"y": 21.275, "r": 22.55, "d": 8.583333},{"x": -110.5,"y": 35.3, "r": 37.849999999999994, "d": 21.333334}], "centers": [{"x": -127.30000000000001,"y": 44.22499999999999, "r": 50.59999999999998, "d": 1.8},{"x": -97.89999999999998,"y": 40.39999999999999, "r": 42.94999999999999, "d": 11.0},{"x": -83.19999999999997,"y": 35.3, "r": 45.499999999999986, "d": 24.0625},{"x": -102.1,"y": 27.650000000000002, "r": 30.200000000000003, "d": 13.785714},{"x": -95.79999999999998,"y": 48.04999999999998, "r": 50.59999999999998, "d": 20.375},{"x": -118.9,"y": 39.12499999999999, "r": 50.59999999999998, "d": 11.333333},{"x": -104.19999999999999,"y": 21.275, "r": 22.55, "d": 8.583333}], "centers": [{"x": -104.19999999999999,"y": 21.275, "r": 22.55, "d": 8.583333},{"x": -85.29999999999998,"y": 35.3, "r": 45.499999999999986, "d": 37.125},{"x": -97.89999999999998,"y": 40.39999999999999, "r": 42.94999999999999, "d": 11.0},{"x": -102.1,"y": 27.650000000000002, "r": 30.200000000000003, "d": 13.785714},{"x": -95.79999999999998,"y": 48.04999999999998, "r": 50.59999999999998, "d": 20.375},{"x": -81.09999999999997,"y": 40.39999999999999, "r": 45.499999999999986, "d": 47.0},{"x": -118.9,"y": 39.12499999999999, "r": 50.59999999999998, "d": 11.333333}], "centers": [{"x": -118.9,"y": 39.12499999999999, "r": 50.59999999999998, "d": 11.333333},{"x": -85.29999999999998,"y": 35.3, "r": 45.499999999999986, "d": 37.125},{"x": -123.1,"y": 34.02499999999999, "r": 48.04999999999998, "d": 11.818182},{"x": -97.89999999999998,"y": 40.39999999999999, "r": 42.94999999999999, "d": 11.0},{"x": -95.79999999999998,"y": 48.04999999999998, "r": 50.59999999999998, "d": 20.375},{"x": -81.09999999999997,"y": 40.39999999999999, "r": 45.499999999999986, "d": 47.0},{"x": -99.99999999999999,"y": 21.275, "r": 22.55, "d": 10.3},{"x": -97.89999999999998,"y": 27.650000000000002, "r": 30.200000000000003, "d": 19.2}], "centers": [{"x": -104.19999999999999,"y": 23.825000000000003, "r": 25.1, "d": 18.75},{"x": -118.9,"y": 37.849999999999994, "r": 48.04999999999998, "d": 12.75},{"x": -85.29999999999998,"y": 35.3, "r": 45.499999999999986, "d": 37.125},{"x": -99.99999999999999,"y": 27.650000000000002, "r": 30.200000000000003, "d": 24.0},{"x": -95.79999999999998,"y": 48.04999999999998, "r": 50.59999999999998, "d": 20.375},{"x": -81.09999999999997,"y": 40.39999999999999, "r": 45.499999999999986, "d": 47.0},{"x": -123.1,"y": 35.3, "r": 45.499999999999986, "d": 16.25},{"x": -97.89999999999998,"y": 41.67499999999999, "r": 42.94999999999999, "d": 22.0},{"x": -95.79999999999998,"y": 21.275, "r": 22.55, "d": 12.875}], "centers": [{"x": -85.29999999999998,"y": 41.67499999999999, "r": 45.499999999999986, "d": 33.333332},{"x": -110.5,"y": 32.75, "r": 37.849999999999994, "d": 25.0},{"x": -106.29999999999998,"y": 35.3, "r": 40.39999999999999, "d": 20.5},{"x": -123.1,"y": 34.025, "r": 40.39999999999999, "d": 26.0},{"x": -89.49999999999997,"y": 21.275, "r": 22.55, "d": 20.0},{"x": -114.69999999999999,"y": 46.774999999999984, "r": 50.59999999999998, "d": 8.333333},{"x": -99.99999999999999,"y": 48.04999999999998, "r": 50.59999999999998, "d": 40.75},{"x": -118.9,"y": 36.574999999999996, "r": 42.94999999999999, "d": 20.4},{"x": -91.59999999999998,"y": 48.04999999999998, "r": 50.59999999999998, "d": 25.0},{"x": -81.09999999999997,"y": 40.39999999999999, "r": 45.499999999999986, "d": 47.0},{"x": -114.69999999999999,"y": 36.574999999999996, "r": 42.94999999999999, "d": 29.4},{"x": -97.89999999999998,"y": 23.825000000000003, "r": 25.1, "d": 30.0},{"x": -123.1,"y": 36.574999999999996, "r": 42.94999999999999, "d": 26.0},{"x": -110.5,"y": 26.375, "r": 27.650000000000002, "d": 20.2},{"x": -97.89999999999998,"y": 41.67499999999999, "r": 42.94999999999999, "d": 22.0},{"x": -93.69999999999999,"y": 44.22499999999999, "r": 45.499999999999986, "d": 33.333332},{"x": -87.39999999999998,"y": 30.200000000000003, "r": 32.75, "d": 24.5},{"x": -110.5,"y": 45.499999999999986, "r": 50.59999999999998, "d": 39.75},{"x": -99.99999999999999,"y": 28.925000000000004, "r": 30.200000000000003, "d": 23.0}], "centers": [{"x": -95.79999999999998,"y": 36.574999999999996, "r": 37.849999999999994, "d": 35.0},{"x": -123.1,"y": 35.3, "r": 37.849999999999994, "d": 65.0},{"x": -83.19999999999997,"y": 34.025, "r": 35.3, "d": 49.5},{"x": -99.99999999999999,"y": 36.574999999999996, "r": 37.849999999999994, "d": 35.0},{"x": -95.79999999999998,"y": 44.22499999999999, "r": 45.499999999999986, "d": 50.0},{"x": -85.29999999999998,"y": 22.55, "r": 25.1, "d": 50.0},{"x": -108.39999999999999,"y": 34.025, "r": 35.3, "d": 91.0},{"x": -87.39999999999998,"y": 21.275, "r": 22.55, "d": 50.0},{"x": -116.8,"y": 39.12499999999999, "r": 40.39999999999999, "d": 23.5},{"x": -104.19999999999999,"y": 26.375, "r": 27.650000000000002, "d": 50.0},{"x": -81.09999999999997,"y": 22.55, "r": 25.1, "d": 50.0},{"x": -91.59999999999998,"y": 39.12499999999999, "r": 40.39999999999999, "d": 50.0},{"x": -114.69999999999999,"y": 48.04999999999998, "r": 50.59999999999998, "d": 12.5},{"x": -114.69999999999999,"y": 40.39999999999999, "r": 42.94999999999999, "d": 23.5},{"x": -81.09999999999997,"y": 42.94999999999999, "r": 45.499999999999986, "d": 50.0},{"x": -99.99999999999999,"y": 46.774999999999984, "r": 48.04999999999998, "d": 78.0},{"x": -97.89999999999998,"y": 27.650000000000002, "r": 30.200000000000003, "d": 96.0},{"x": -89.49999999999997,"y": 37.849999999999994, "r": 40.39999999999999, "d": 50.0},{"x": -89.49999999999997,"y": 40.39999999999999, "r": 42.94999999999999, "d": 50.0},{"x": -93.69999999999999,"y": 48.04999999999998, "r": 50.59999999999998, "d": 50.0},{"x": -85.29999999999998,"y": 40.39999999999999, "r": 42.94999999999999, "d": 50.0},{"x": -95.79999999999998,"y": 41.67499999999999, "r": 42.94999999999999, "d": 50.0},{"x": -87.39999999999998,"y": 41.67499999999999, "r": 42.94999999999999, "d": 50.0},{"x": -102.1,"y": 27.650000000000002, "r": 30.200000000000003, "d": 50.0},{"x": -102.1,"y": 42.94999999999999, "r": 45.499999999999986, "d": 16.5},{"x": -112.6,"y": 39.12499999999999, "r": 40.39999999999999, "d": 23.5},{"x": -85.29999999999998,"y": 42.94999999999999, "r": 45.499999999999986, "d": 50.0},{"x": -81.09999999999997,"y": 37.849999999999994, "r": 40.39999999999999, "d": 94.0},{"x": -97.89999999999998,"y": 37.849999999999994, "r": 40.39999999999999, "d": 35.0},{"x": -81.09999999999997,"y": 45.499999999999986, "r": 48.04999999999998, "d": 50.0},{"x": -110.5,"y": 45.499999999999986, "r": 48.04999999999998, "d": 79.5},{"x": -116.8,"y": 34.025, "r": 35.3, "d": 101.0},{"x": -85.29999999999998,"y": 48.04999999999998, "r": 50.59999999999998, "d": 57.0},{"x": -87.39999999999998,"y": 39.12499999999999, "r": 40.39999999999999, "d": 50.0},{"x": -99.99999999999999,"y": 23.825000000000003, "r": 25.1, "d": 75.0},{"x": -93.69999999999999,"y": 40.39999999999999, "r": 42.94999999999999, "d": 50.0},{"x": -91.59999999999998,"y": 44.22499999999999, "r": 45.499999999999986, "d": 50.0},{"x": -93.69999999999999,"y": 42.94999999999999, "r": 45.499999999999986, "d": 50.0},{"x": -83.19999999999997,"y": 31.475, "r": 32.75, "d": 99.0},{"x": -83.19999999999997,"y": 21.275, "r": 22.55, "d": 50.0},{"x": -91.59999999999998,"y": 41.67499999999999, "r": 42.94999999999999, "d": 50.0},{"x": -85.29999999999998,"y": 35.3, "r": 37.849999999999994, "d": 49.5},{"x": -83.19999999999997,"y": 44.22499999999999, "r": 45.499999999999986, "d": 50.0},{"x": -93.69999999999999,"y": 45.499999999999986, "r": 48.04999999999998, "d": 50.0},{"x": -87.39999999999998,"y": 34.025, "r": 35.3, "d": 49.5},{"x": -91.59999999999998,"y": 46.774999999999984, "r": 48.04999999999998, "d": 50.0},{"x": -83.19999999999997,"y": 41.67499999999999, "r": 42.94999999999999, "d": 50.0},{"x": -97.89999999999998,"y": 35.3, "r": 37.849999999999994, "d": 35.0}]]}')
var testData = JSON.parse('{"centers": [{"x": 37.787, "y": -122.228, "r": 37.60}]}')
var rawData1 = {"clusters": [testData, testData, testData, testData, testData, testData, testData, testData, testData, testData]};
var rawData2 = rawData1
var rawData3 = rawData1
  //TODO this has to be generated by calling the correct method!
var data = JSON.parse('{"clusters":[{"centers": [{ "x": 250, "y": 40, "r": 10 }, { "x": 40, "y": 75, "r": 43 }]}, {"centers": [{ "x": 759, "y": 255, "r": 36 }]}, {"centers": [{ "x": 50, "y": 40, "r": 10 }, { "x": 60, "y": 20, "r": 10 }]}, {"centers": [{ "x": 133, "y": 54, "r": 12 }, { "x": 40, "y": 75, "r": 43 }]}, {"centers":[{ "x": 260, "y": 120, "r": 10 }, { "x": 40, "y": 75, "r": 13 }, { "x": 33, "y": 54, "r": 12 }]}]}');

  //Graphical environnement
var graph = document.getElementById("map").getElementsByTagName("svg")[0].appendChild(po.svg("g"));


function cluster(view_center, zoom, data) {
  rawData1 = JSON.parse(data); update2();
  if(view_center) map.center(view_center);
  if(zoom) map.zoom(zoom);
}
//Translating function 
  /*@brief: Transforms the raw data in pixels
    @expects: [{x, y, r}, {x,y,r}...]
  */
function generateData(centers){
  var res = [];
  for(var i = 0; i < centers.length; i++) {
    var e = centers[i];
    var bitch = JSON.parse('{"x": '+e.r+', "y": '+e.y+'}');
    var border = geoToPx(bitch);
    var toPix = geoToPx(e);
    var diff = border.y - toPix.y;
    var nR = Math.abs(border.y - toPix.y);
    
    res.push(JSON.parse('{"x":'+toPix.x+', "y":'+toPix.y+', "r": '+nR+'}'));
    
  }
  return res;
}

//Functions to draw
  //Expects input of the form {"x": _, "y": _, "r": _}
var colors = ["", "LightCoral", "RoyalBlue", "Purple"]
function drawCircle(entry, colorID) {
  console.log("color here: "+colorID)
  var point = graph.appendChild(po.svg("circle"));
  console.log("chingjjv"+colorID)
  point.setAttribute('style', 'fill:'+ colors[colorID] +"; "+point.getAttribute("fill"));
  point.setAttribute('style', 'opacity:'+Math.random()/3/*entry.d*/+"; "+point.getAttribute("style"));
  point.setAttribute("cx", entry.x);
  point.setAttribute("cy", entry.y);
  point.setAttribute("r", entry.r);
}

  //Goes through the list of centers at one level
function drawCenters(clusts, colorID){
  console.log("color: "+colorID)
  for(var i = 0; i < clusts.length; i++) {
    console.log("color: "+colorID)
    drawCircle(clusts[i], colorID);
  }
}


//Helper functions
  //Translates pixels {"x": _, "y":_, "r":_ } into geolocation 
  //using only the x and y attribute; Returns {"lat":_, "lon":_}
function pxToGeo(pt){
  return map.pointLocation(JSON.parse('{"x":'+ pt.x+', "y": '+pt.y+'}'));
}

  //Translates geolocation {"lat": _, "lon":_} into pixel {"x":_, "y": _}
function geoToPx(pt){
  //Trouble parsing here
  return map.locationPoint(JSON.parse('{ "lat":'+pt.x+', "lon": '+pt.y+'}'));
}

function update2() {
  //custers: 0 (more details) ->  9
  //zoom level:  3 (far zoom) -> 10
  while(graph.firstChild){
    graph.removeChild(graph.firstChild);
  }
  drawCenters(generateData(rawData1.clusters[10 - Math.floor(map.zoom())].centers), 1);
  drawCenters(generateData(rawData2.clusters[10 - Math.floor(map.zoom())].centers), 2);
  drawCenters(generateData(rawData3.clusters[10 - Math.floor(map.zoom())].centers), 3);
}


  //Tests drawCenters
drawCenters(generateData(rawData1.clusters[0].centers), 1);
  
  //Tests
var res = geoToPx(JSON.parse('{"x": 37.787, "y": -122.228}'));
var res2 = geoToPx(JSON.parse('{"x": 37.787, "y": -132.228}'));
//var res2 = pxToGeo(JSON.parse('{"x": 758.9, "y": 160}'));
console.log("The x: "+res.x+" the y: "+res.y);
console.log("The x2:"+res2.x+" the y2 "+res2.y);

//drawCircle(JSON.parse('{"x": 0, "y": 300, "r": 30}'))

