
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
center.lat = -122.5198
center.lon = 37.6335

var mapborders = [];
//map tiles initialization
map.add(po.image()
    .url(po.url("http://{S}tile.cloudmade.com"
    + "/1a1b06b230af4efdbb989ea99e9841af" // http://cloudmade.com/register
    + "/998/256/{Z}/{X}/{Y}.png")
    .hosts(["a.", "b.", "c.", ""])));

// +/- zoom buttons on map
map.add(po.compass()
    .pan("none")
);

function debug(object) {
	console.log(JSON.stringify(object));//, null, 4));
}

var coordinates_array = [];

/**
 * converts {"x": screenCoordX, "y": screenCoordY}
 * to {"lat": geoLatitude, "lon": geoLongitude}
 */
function pxToGeo(json) {
  return map.pointLocation(json);
}

/**
  * Code Adrien
  *
  */
 
 //Graphical environnement
var graph = document.getElementById("map").getElementsByTagName("svg")[0].appendChild(po.svg("g"));

//Translating function 
  /*@brief: Transforms the raw data in pixels
    @expects: [{x, y, r}, {x,y,r}...]
  */
function generateData(centers){
  var res = [];
  for(var i = 0; i < centers.length; i++) {
    var e = centers[i], copy = e;
    copy.y = copy.y + copy.r;
    var toPix = geoToPx(e), nR = Math.abs(geoToPx(copy).y - toPix.y);
    res.push(JSON.parse('{"x":'+toPix.x+', "y":'+toPix.y+', "r": '+nR+'}'));
  }
  return res;
}

//Functions to draw
  //Expects input of the form {"x": _, "y": _, "r": _}
function drawCircle(entry) {
  var point = graph.appendChild(po.svg("circle")); 
  point.setAttribute("cx", entry.x);
  point.setAttribute("cy", entry.y);
  point.setAttribute("r", entry.r);
}

  //Goes through the list of centers at one level
function drawCenters(clusts){
  for(var i = 0; i < clusts.length; i++) {
    drawCircle(clusts[i]);
  }
}


//Helper functions
  //Translates pixels {"x": _, "y":_, "r":_ } into geolocation 
  //using only the x and y attribute; Returns {"lat":_, "lon":_}
function pxToGeo2(pt){
  return map.pointLocation(JSON.parse('{"x":'+ pt.x+', "y": '+pt.y+'}'));
}

  //Translates geolocation {"lat": _, "lon":_} into pixel {"x":_, "y": _}
function geoToPx(pt){
  //Trouble parsing here
  return map.locationPoint(JSON.parse('{ "lat":'+pt.x+', "lon": '+pt.y+'}'));
}

/**
  * END Code Adrien
  *
  */

/*
 * build the initial map with a corresponding view as well as the selected regions if any
 */
function mapReCenter(viewCenter, mapZoom) {
	map.center(viewCenter);
	map.zoom(mapZoom);
}

var densityRegionList = [];
var regions1//TODO: set to []
var regions2
var regions3
var regionsF1 = true;
var regionsF2 = false;
var regionsFCombined = false; //only show intersection by default
function mapResult() {
	while (densityRegionList.length != 0)
		map.remove(densityRegionList.pop())
	if (regionsF1)
		showRegionIntesity(regions1, 1)
	if (regionsF2)
		showRegionIntesity(regions2, 2)
	if (regionsFCombined)
		showRegionIntesity(regions3, 3)
}

function showRegionIntesity(regionList, color) {
	console.log("in showRegionIntesity:"+regionList.length)
	console.log("function showRegionIntesity")

	console.log("length:"+regionList.length)
	for (index = 0; index < regionList.length; ++index) {
		var elem = regionList[index]
		//console.log("iter:"+JSON.stringify(elem))
		if (elem)
			addNewSubRegion(elem[0], elem[1], elem[2], color) //save the regions
	}
}

/**
 *  calculates and stores the geographical information corresponding to the current view of the map
 */
var topCorner;
var bottomCorner;
function update() {
	if  (!map) {
		setTimeout(function() {update() }, 100) //busy wait while map is not yet loaded
	}
	var elem;
	topCorner = pxToGeo(JSON.parse('{"x":0, "y": 0}')); //TODO: pass to Play!
	bottomCorner = pxToGeo(map.size());
	elem = document.getElementById("focussed")
	if (elem)
		elem.value = JSON.stringify([topCorner, bottomCorner])
	elem = document.getElementById("viewCenter")
	if (elem)
		elem.value = JSON.stringify(map.center())
	elem = document.getElementById("zoomLevel")
	if (elem)
		elem.value = map.zoom()
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
 * When we start a click-and-drag action and the user selected to create a new region (i.e. flag)
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
    start.x =  window.event.clientX - 2; // Addition of some corrector factors (due to the design)
    start.y = window.event.clientY - 125; // Addition of some corrector factors (due to the design)
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
      newPos.x = window.event.clientX - start.x - 2;
      if (newPos.x < 0)
        newPos.x = 5;
      newPos.y = window.event.clientY - start.y - 125;
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
      var bottomRight = pxToGeo(JSON.parse('{"x":'+end.x+', "y": '+end.y+'}'))
      coordinates_array.push([topLeft,bottomRight]);
      //coordinates.value = jCoordinates
      document.getElementById("coordinates").value = JSON.stringify(coordinates_array)
      //console.log(coordinates.value)
      //debug(coordinates.value)
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

function regionCheckbox(region, checkbox) {
	switch(region) {
		case 1:
			regionsF1 = checkbox.checked;
			break;
		case 2:
			regionsF2 = checkbox.checked;
			break;
		case 3:
			regionsFCombined = checkbox.checked;
			break;
		//default:
	}
	mapResult() //redraw all regions! ->
}


function addNewRegions(regionList) {
	var i = 0;
  //alert(regionList.length)
	while (i < regionList.length) {
		addNewRegion(regionList[i][0], regionList[i][1])
		i++;
	}
	console.log("done")
}

/*
 * function to add a region with opacity linked to the number of tweets in this region
 */
function addNewSubRegion(topLeft, bottomRight, opacity, color) {
  var container = divToGeoJson(topLeft, bottomRight);
  container.setAttribute("class", container.getAttribute("class")+" subregion id"+idcount);
  container.setAttribute('style', 'opacity:'+opacity+"; "+container.getAttribute("style"));
  var myFill;
  switch(color) {
	case 1:
		fill = "#1f77b4";
		break;
	case 2:
		fill = "#ff7f0e";
		break;
	case 3:
		fill = "#C2AB93";
		break;
	default:
		fill = "red"
  }
  container.setAttribute('fill', fill);
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
  densityRegionList.push(region);
  return region.container()
}

/*
 * NOT FUNCTIONING YET -> IS IT NEEDED?
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
  document.getElementById("coordinates").value = "";
}
