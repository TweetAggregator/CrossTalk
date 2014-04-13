var po = org.polymaps;

var map = po.map()
var div = document.getElementById("map"),
    svg = div.appendChild(po.svg("svg"));
map.container(svg);

var interact = po.interact(); //create separate object for the focus so we can remove it when drawing a rectangle
map.add(interact); //enable move and zoom events
map.on("resize", update);
map.on("move", update);
map.add(po.geoJson()
    .features([{geometry: {coordinates: [-122.258, 37.805], type: "Point"}}])
    .on("load", load));
//map tiles initialization
map.add(po.image()
    .url(po.url("http://{S}tile.cloudmade.com"
    + "/1a1b06b230af4efdbb989ea99e9841af" // http://cloudmade.com/register
    + "/998/256/{Z}/{X}/{Y}.png")
    .hosts(["a.", "b.", "c.", ""])));

// +/- buttons on map
map.add(po.compass()
    .pan("none"));



/* 
  converts {"x": screenCoordX, "y": screenCoordY}
  to {"lat": geoLatitude, "lon": geoLongitude}
*/
function pxToGeo(json) {
  return map.pointLocation(json);
}

/*
  updates the geographical data of the current view
*/
function update() {
  var topR = pxToGeo(JSON.parse('{"x":0, "y": 0}'));
  var bottomL = pxToGeo(map.size());
  /*var myString = "<br/>lat 0: ".concat(topR.lat.toString()) +
    "<br/>lon: ".concat(topR.lon) +
    "<br/>lat".concat(bottomL.lat) +
    "<br/>lon".concat(bottomL.lon);
  document.getElementById('myDiv').innerHTML = myString ;*/
}

function load(e) {
  var r = 20 * Math.pow(2, e.tile.zoom - 12);
  for (var i = 0; i < e.features.length; i++) {
    var c = n$(e.features[i].element),
        g = c.parent().add("svg:g", c);
    g.add(c
        //.attr("fill", "url(#r1)")
        .attr("cx", r)
        .attr("cy", r)
        );
  }
}

/*
 * is called whenever the user chooses to create a new region (e.g. on button press)
 */
var flag = 0; // tells the listener functions that the user is drawing a new region
function drawRegion() {
  map.remove(interact); //remove the dragging focus from the map
  flag = 1;
}
var circleFlag =0;
function drawCircleRegion(){
	map.remove(interact);
	circleFlag=1;
}

/*
 * a set of listener function wich are responsible for drawing the rectangle and sticking it to the map
 */
// "svg" can hold children "g" which hold graphics to be shown on top of the map
var g; //each rectangle has it's personal "g" element
var rect; //temporary element for drawing before we can
var circle;
var newCircleFlag=0;
var center_x;
var center_y;
var count = 0;
var newRegionFlag = 0; // enables to resize the rectangle 
var startX = 0; //in pixles
var startY = 0; //in pixles
//insert a new, small-sized div to start with
div.addEventListener("mousedown", function(){
  if (flag == 1) {
    g = document.getElementById("map").getElementsByTagName("svg")[0].appendChild(po.svg("g"));
    rect = g.appendChild(po.svg("rect"))
    rect.setAttribute("width", 5);
    rect.setAttribute("height", 5);
    startX = window.event.clientX;
    startY = window.event.clientY;
    g.setAttribute("transform", "translate(" + startX + "," + startY + ")");
    newRegionFlag = 1;
  }
}, false);
//resize the div -> can only be done from top right to bottom left
div.addEventListener("mousemove", function(){
    if (flag == 1 && newRegionFlag == 1) {
      var newX = window.event.clientX - startX;
      if (newX < 0)
        newX = 5;
      var newY = window.event.clientY - startY;
      if (newY < 0)
        newY = 5;
      rect.setAttribute("width", newX);
      rect.setAttribute("height", newY);
    }
}, false);
//finalizes the rectangle
div.addEventListener("mouseup", function(){
    if (flag == 1) {
      newRegionFlag = 0;
      flag = 0;
      document.getElementById("map").getElementsByTagName("svg")[0].removeChild(g);
      //g.removeChild(rect); //the svg element is delete (it does not stick to the map)
      //a new element, which sticks to the map, gets created
      var endX = startX+parseInt(rect.getAttribute("width"));
      var endY = startY+parseInt(rect.getAttribute("height"));
      var topLeft = pxToGeo(JSON.parse('{"x":'+startX+', "y": '+startY+'}'))
      var bottomRight = pxToGeo(JSON.parse('{"x":'+endX+', "y": '+endY+'}'))

      addNewRegion(topLeft, bottomRight)

      map.add(interact)   
  }
}, false);
//insert a new, small-sized div (circle) to start with
div.addEventListener("mousedown", function(){
  if (circleFlag == 1) {
    g = document.getElementById("map").getElementsByTagName("svg")[0].appendChild(po.svg("g"));
    circle = g.appendChild(po.svg("circle"))
    var center_x = window.event.clientX;
    var center_y = window.event.clientY;
    circle.setAttribute("x_center", center_x); // center is 5px left to the clic
    circle.setAttribute("y_center", center_y); // center ""
    circle.setAttribute("ray",5); // start with r = 5

    g.setAttribute("transform", "translate(" + center_x + "," + center_y + ")");
    newRegionCircleFlag = 1;
  }
}, false);
//resize the div -> can only be done from top right to bottom left
div.addEventListener("mousemove", function(){
    if (circleFlag == 1 && newRegionCircleFlag==1) {
    	var newRay = window.event.clienX - center_x;

    	circle.setAttribute("ray",newRay);
      // var newX = window.event.clientX - startX;
      // if (newX < 0)
      //   newX = 5;
      // var newY = window.event.clientY - startY;
      // if (newY < 0)
      //   newY = 5;
      // rect.setAttribute("width", newX);
      // rect.setAttribute("height", newY);
    }
}, false);
//finalizes the rectangle
div.addEventListener("mouseup", function(){
    if (circleFlag == 1) {
      newRegionCircleFlag = 0;
      circleFlag= 0;
      document.getElementById("map").getElementsByTagName("svg")[0].removeChild(g);

      //g.removeChild(rect); //the svg element is delete (it does not stick to the map)
      //a new element, which sticks to the map, gets created
/*      var endX = startX+parseInt(rect.getAttribute("width"));
      var endY = startY+parseInt(rect.getAttribute("height"));
      var topLeft = pxToGeo(JSON.parse('{"x":'+startX+', "y": '+startY+'}'))
      var bottomRight = pxToGeo(JSON.parse('{"x":'+endX+', "y": '+endY+'}'))*/
      var finalRay = center_x+500;
      addCircleNewRegion(center_x,center_y,finalray);
      map.add(interact)   
  }
}, false);

/*
 * inserts a new region on the map, given starting and ending longitude and latitude
 * -> could also be used to show saved regions
 */
var vennDiv = document.getElementById("venns");
function addNewRegion(topLeft, bottomRight) {
  var region = po.geoJson()
    .features([{geometry: {coordinates:
      [[[topLeft.lon, topLeft.lat],
      [topLeft.lon, bottomRight.lat],
      [bottomRight.lon, bottomRight.lat],
      [bottomRight.lon, topLeft.lat],
      [0, 0]]], type: "Polygon"}}])
  var container = region.container()
  container.setAttribute("class", container.getAttribute("class")+" region id"+count);
  container.setAttribute("onClick", "removeThis(e)"); //does not work so far
  
  var div = document.createElement('div');
  div.id = "id"+count;
  div.innerHTML = topLeft+" "+bottomRight;
  console.log(vennDiv);
  console.log(div);
  var venns = document.getElementsByClassName("venns");
  console.log(venns);
  console.log(venns[0]);
  console.log(venns);
  //venns[0].appendChild(div);
  count++;
  map.add(region).on("load", load);
}
function addCircleNewRegion(cx,cy,r){
	var circleRegion = po.geoJson.features([{geometry: {coordinates:[cx,cy],type:"Point"}}])
	var circlecontainer = circleRegion.container();

  circleContainer.setAttribute("class", circleContainer.getAttribute("class")+" region id"+count);
  circleContainer.setAttribute("onClick", "removeThis(e)"); //does not work so far
	var geojsonFeature = {
    "type": "Feature",
    "properties": {
        "name": "Coors Field",
        "amenity": "Baseball Stadium",
        "popupContent": "This is where the Rockies play!"
    },

};
var geojsonMarkerOptions = {
    radius: r,
    fillColor: "#ff7800",
    color: "#000",
    weight: 1,
    opacity: 1,
    fillOpacity: 0.8
};

L.geoJson(geojsonFeature, {
    pointToLayer: function (feature, latlng) {
        return L.circleMarker(latlng, geojsonMarkerOptions);
    }
}).addTo(map);
}

//zoom level -> cluster
function showCircles(listOfList) {

}

function showCircle(geoCenter, radius) {

}

function removeThis(e) {
  consol.log("heelo");
  e = e.target || e.srcElement;
  e.parentNode.removeChild(e)
}

/*
 * I think the map layer intercept all clicks...
 */
function reset() {
  var regions = document.getElementsByClassName('layer region');
  while (regions[0]) {
     regions[0].parentNode.removeChild(regions[0]);
  }
}

function debug() {
  var x = pxToGeo(JSON.parse('{"x":'+100+', "y": '+100+'}'));
  var y = pxToGeo(JSON.parse('{"x":'+1000+', "y": '+500+'}'));
  addNewRegion(x, y);
  x = pxToGeo(JSON.parse('{"x":'+200+', "y": '+50+'}'));
  y = pxToGeo(JSON.parse('{"x":'+800+', "y": '+400+'}'));
  addNewRegion(x, y);
  x = pxToGeo(JSON.parse('{"x":'+900+', "y": '+20+'}'));
  y = pxToGeo(JSON.parse('{"x":'+1000+', "y": '+40+'}'));
  addNewRegion(x, y);
  x = pxToGeo(JSON.parse('{"x":'+1000+', "y": '+40+'}'));
  y = pxToGeo(JSON.parse('{"x":'+1100+', "y": '+60+'}'));
  addNewRegion(x, y);
}
debug();

//map.on("update", update);
