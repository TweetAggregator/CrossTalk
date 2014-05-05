
var po = org.polymaps;

var map = po.map()
var div = document.getElementById("map"),
    svg = div.appendChild(po.svg("svg"));
map.container(svg);
var interact = po.interact(); //create separate object for the focus so we can remove it when drawing a rectangle
map.add(interact); //enable move and zoom events
map.on("resize", update);
map.on("move", update);
var center = new Object;
center.lat = 55.9351
center.lon = 6.6335
//map.center(center)
//map.zoom(21)

//map tiles initialization
map.add(po.image()
    .url(po.url("http://{S}tile.cloudmade.com"
    + "/1a1b06b230af4efdbb989ea99e9841af" // http://cloudmade.com/register
    + "/998/256/{Z}/{X}/{Y}.png")
    .hosts(["a.", "b.", "c.", ""])));

// +/- zoom buttons on map
map.add(po.compass()
    .pan("none"));

function debug(object) {
	console.log(JSON.stringify(object));//, null, 4));
}

var coordinates_array = [];

/* 
  converts {"x": screenCoordX, "y": screenCoordY}
  to {"lat": geoLatitude, "lon": geoLongitude}
*/
function pxToGeo(json) {
  return map.pointLocation(json);
}

/*
 * build the initial map with a corresponding view as well as the selected regions if any
 */
var VC;
var ZL; 
function reload(viewCenter, mapZoom, regionList) {
	if (!map) {
		console.log("update() busy wait loop")
		setTimeout(function() {reload(viewCenter, zoom, regionList) }, 10) //busy wait while map is not yet loaded
	}
	console.log("reload:")
	console.log(viewCenter) //JSON.toJSON does NOT work!
	console.log("zoom:"+mapZoom)
	console.log("to be added:"+regionList)
	addNewRegions(regionList)
	console.log("XXX before")
	VC = viewCenter
	ZL = zoomLevel
	var x = map.center(viewCenter)
	console.log("XXX middle")
	var y = map.zoom(mapZoom);
	console.log("XXX after")
}
/*
 *  calculates and stores the geographical information corresponding to the current view of the map
 */
var topCorner;
var bottomCorner;
function update() {
	//console.log(map.center())
	//console.log(map.zoom())
	if (!map || !document.getElementById("viewCenter") || !document.getElementById("zoomLevel")) {
		console.log("update() busy wait loop")
		setTimeout(function() {update() }, 10) //busy wait while map is not yet loaded
	}
	topCorner = pxToGeo(JSON.parse('{"x":0, "y": 0}'));
	bottomCorner = pxToGeo(map.size());
	console.log(topCorner)
	console.log(bottomCorner)
	console.log(map.center())
	document.getElementById("viewCenter").value = JSON.stringify(map.center())
	document.getElementById("zoomLevel").value = map.zoom()
}

/*
 * is called whenever the user chooses to create a new region (e.g. on button press)
 */
var flag = 0; // tells the listener functions that the user is drawing a new region (instead of just moving the cursor)
function drawRegion() {
  map.remove(interact); //remove the dragging focus from the map
  flag = 1;
}

/*
 * a set of listener function wich are responsible for drawing the rectangle and sticking it to the map
 */
// "svg" can hold children
// "g" holds graphics to be shown on top of the map
var g; //each rectangle has it's personal graphics DOM element
var rect; //temporary element for drawing before we can stick it to the map
var idcount = 0; //if counter 
var newRegionFlag = 0; // enables to resize the rectangle 
var start = new Object(); //x, y in pixles
/*
 * When we start a click-and-drag action and the user slected to create a new region (i.e. flag)
 * we start by creating a container on top of the map such that the user can see what he is doing 
 */
div.addEventListener("mousedown", function(){
  if (flag == 1) {
    g = document.getElementById("map").getElementsByTagName("svg")[0].appendChild(po.svg("g"));
    rect = g.appendChild(po.svg("rect"))
    //set initial size of the rectangle
    rect.setAttribute("width", 5);
    rect.setAttribute("height", 5);
    //fix the initial position of the rectangle, drawing is only possible top-down, left-right
    start.x =  window.event.clientX;
    start.y = window.event.clientY;
    g.setAttribute("transform", "translate(" + start.x + "," + start.y + ")"); //I don't know why we need this
    newRegionFlag = 1;
  }
}, false);

/*
 * resize the div before pinning it to the map
 * resize the container as long as the dragging goes on
 */
div.addEventListener("mousemove", function() {
    if (flag == 1 && newRegionFlag == 1) {
      var newPos = new Object();
      newPos.x = window.event.clientX - start.x;
      if (newPos.x < 0)
        newPos.x = 5;
      newPos.y = window.event.clientY - start.y;
      if (newPos.y < 0)
        newPos.y = 5;
      rect.setAttribute("width", newPos.x);
      rect.setAttribute("height", newPos.y);
    }
}, false);

/*
 * finalizing the newly added the rectangle, i.e. pinning it to the map
 */
div.addEventListener("mouseup", function(){
    if (flag == 1) {
      newRegionFlag = 0;
      flag = 0;
      document.getElementById("map").getElementsByTagName("svg")[0].removeChild(g); //remove the div from the map and replace it by a GeoJSON element
      g.removeChild(rect); //the svg element is delete (it does not stick to the map)
      //a new element, which sticks to the map, gets created
      var end = new Object();
      end.x = start.x+parseInt(rect.getAttribute("width"));
      end.y = start.y+parseInt(rect.getAttribute("height"));
      var topLeft = pxToGeo(JSON.parse('{"x":'+start.x+', "y": '+start.y+'}'))
     // var topRight = pxToGeo(JSON.parse('{"x":'+start.x+', "y": '+end.y+'}'))
      var bottomRight = pxToGeo(JSON.parse('{"x":'+end.x+', "y": '+end.y+'}'))
     // var bottomLeft = pxToGeo(JSON.parse('{"x":'+end.x+', "y": '+start.y+'}'))
      coordinates_array.push([topLeft,bottomRight]);
      var jCoordinates = JSON.stringify(coordinates_array)
      //coordinates.value = jCoordinates
      document.getElementById("coordinates").value = jCoordinates
      //console.log(coordinates.value)
      //debug(coordinates.value)
      alert(jCoordinates)
      addNewRegion(topLeft, bottomRight)
      map.add(interact) //put back move and resize focus on the map
      
  }
}, false);

/*
 * inserts a new region on the map, given starting and ending longitude and latitude
 * -> could also be used to show saved regions
 */
var vennDiv = document.getElementById("venns");
function addNewRegion(topLeft, bottomRight) {
  var container = divToGeoJson(topLeft, bottomRight)
  container.setAttribute("class", container.getAttribute("class")+" region id"+idcount);
  container.setAttribute("onClick", "removeThis(e)"); //does not work so far
  

}

function addNewRegions(regionList) {
	var i = 0;
	while (i < regionList.length) {
		addNewRegion(regionList[i][0], regionList[i][1])
		i++;
	}
	console.log("done")
}

/*
 * function to add a region with opacity linked to the number of tweets in this region
 */
function addNewSubRegion(topLeft, bottomRight, opacity) {
  var container = divToGeoJson(topLeft, bottomRight);
  container.setAttribute("class", container.getAttribute("class")+" subregion id"+idcount);
  container.setAttribute('style', 'opacity:'+opacity+"; "+container.getAttribute("style"));
}

/*
 * helper function used by addNewRegion and addNewSubRegion to create GeoJSON objects
 */
function divToGeoJson(topLeft, bottomRight) {
  var region = po.geoJson()
    .features([{geometry: {coordinates:
      [[[topLeft.lon, topLeft.lat],
      [topLeft.lon, bottomRight.lat],
      [bottomRight.lon, bottomRight.lat],
      [bottomRight.lon, topLeft.lat],
      [0, 0]]], type: "Polygon"}}]);
  map.add(region);//.on("load", load);
  return region.container()
}

//zoom level -> cluster
function showCircles(listOfList) {

}

/*
 * still have to figure out how to do this is needed
 */ 
function addCircle(geoCenter, radius) {
}

/*
 * NOT FUNCTIONING YET -> IS IT NEEDED
 * function to remove a region when it is clicked
 * I think the map layer intercept all clicks...
 */
function removeThis(e) {
  e = e.target || e.srcElement;
  e.parentNode.removeChild(e)
}

/*
 * 
 */
function reset() {
  var regions = document.getElementsByClassName('layer region');
  while (regions[0]) {
     regions[0].parentNode.removeChild(regions[0]);
  }
  coordinates_array = [];
  coordinates.value = "";
}
/*var testArray = []
/*function debug() {
  testArray.push([3.33,5.22]);
  testArray.push([4.63,73.12]);
  var jsonTestArray = JSON.stringify(testArray);
  var x = pxToGeo(JSON.parse('{"x":'+100+', "y": '+100+'}'));
  var y = pxToGeo(JSON.parse('{"x":'+1000+', "y": '+500+'}'));
  addNewRegion(x, y);
  var middle = JSON.parse('{"lat":'+(x.lat+y.lat)/2+', "lon": '+(x.lon+y.lon)/2+'}');
  var topmiddle = JSON.parse('{"lat":'+x.lat+', "lon": '+(x.lon+y.lon)/2+'}');
  var bottommiddle = JSON.parse('{"lat":'+y.lat+', "lon": '+(x.lon+y.lon)/2+'}');
  var leftmiddle = JSON.parse('{"lat":'+(x.lat+y.lat)/2+', "lon": '+x.lon+'}');
  var rightmiddle = JSON.parse('{"lat":'+(x.lat+y.lat)/2+', "lon": '+y.lon+'}');
  addNewSubRegion(x, middle, Math.random()/4+0.2);
  addNewSubRegion(topmiddle, rightmiddle, Math.random()/4+0.2);
  addNewSubRegion(middle, y, Math.random());
  addNewSubRegion(leftmiddle, bottommiddle, Math.random()/4+0.2);
  var z = JSON.parse('{"lat":'+(x.lat+y.lat)/2+', "lon": '+x.lon+'}');
  addNewSubRegion(z, y, Math.random()/4+0.2);
  x = pxToGeo(JSON.parse('{"x":'+200+', "y": '+50+'}'));
  y = pxToGeo(JSON.parse('{"x":'+800+', "y": '+400+'}'));
  addNewRegion(x, y);
  x = pxToGeo(JSON.parse('{"x":'+900+', "y": '+20+'}'));
  y = pxToGeo(JSON.parse('{"x":'+1000+', "y": '+40+'}'));
  addNewRegion(x, y);
  x = pxToGeo(JSON.parse('{"x":'+1000+', "y": '+40+'}'));
  y = pxToGeo(JSON.parse('{"x":'+1100+', "y": '+60+'}'));
  addNewRegion(x, y);
  // Generate a form
        $("#myform").dform(jsonTestArray);
}


debug(); //only used for debugging */
