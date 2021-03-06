var po = org.polymaps;

var map = po.map()
var div = document.getElementById("map"),
    svg = div.appendChild(po.svg("svg"));
map.container(svg);

var interact = po.interact(); //create separate object for the focus so we can remove it when drawing a rectangle
map.add(interact); //enable move and zoom events
map.on("resize", update);
map.on("move", update);

//map tiles initialization
map.add(po.image()
    .url(po.url("http://{S}tile.cloudmade.com"
    + "/1a1b06b230af4efdbb989ea99e9841af" // http://cloudmade.com/register
    + "/998/256/{Z}/{X}/{Y}.png")
    .hosts(["a.", "b.", "c.", ""])));

// +/- zoom buttons on map
map.add(po.compass()
    .pan("none"));
// global array for string the coordinates of ALL the rectangles
var coordinates_array = [];

/* 
  converts {"x": screenCoordX, "y": screenCoordY}
  to {"lat": geoLatitude, "lon": geoLongitude}
*/
function pxToGeo(json) {
  return map.pointLocation(json);
}

/*
  calculates and stores the geographical information corresponding to the current view of the map
*/
var topCorner;
var bottomCorner;
function update() {
  topCorner = pxToGeo(JSON.parse('{"x":0, "y": 0}'));
  bottomCorner = pxToGeo(map.size());
  var myString = "<br/>lat 0: ".concat(topCorner.lat.toString()) +
    "<br/>lon: ".concat(topCorner.lon) +
    "<br/>lat".concat(bottomCorner.lat) +
    "<br/>lon".concat(bottomCorner.lon);
  //document.getElementById('myDiv').innerHTML = myString ; //show the information in the sidebar
}

/*map.add(po.geoJson()
    .url(crimespotting("http://oakland.crimespotting.org"
        + "/crime-data"
        + "?count=1000"
        + "&format=json"
        + "&bbox={B}"
        + "&dstart=2010-04-01"
        + "&dend=2010-05-01"))
    .on("load", load)
    .clip(false)
    .zoom(14));
function load(e) {
  console.log("LOAD");
  /*var r = 20 * Math.pow(2, e.tile.zoom - 12);
  for (var i = 0; i < e.features.length; i++) {
    var c = n$(e.features[i].element),
        g = c.parent().add("svg:g", c);
  console.log(r);
    g.add(c
        .attr("fill", "url(#r1)")
        .attr("cx", r)
        .attr("cy", r)
        );
  }*/

  /*var cerc = pxToGeo(JSON.parse('{"x":'+700+', "y": '+700+'}'));
  addCircle(cerc, 0);*/
  /*map.add(po.geoJson()
    .features([{geometry: {coordinates: [-122.40, 37.75], type: "Point"}}])
    .on("load", load));*/
  /*var g = e.tile.element;//document.getElementById("map").getElementsByTagName("svg")[0].appendChild(po.svg("g")),
  while (g.lastChild) g.removeChild(g.lastChild);
  var point = g.appendChild(po.svg("circle"));
    point.setAttribute("cx", -122.40);
    point.setAttribute("cy", 37.75);
    point.setAttribute("r", 16);*/
  /* //circle testing
  var cluster = e.tile.cluster || (e.tile.cluster = kmeans()
      .iterations(16)
      .size(64));

  for (var i = 0; i < e.features.length; i++) {
  console.log(e.features[i].data.geometry.coordinates);
    cluster.add(e.features[i].data.geometry.coordinates);
  }
  //cluster.add([-122.215922, 37.803117, {"x": 209.30414364440367, "y": 34.31288245948963]});

  var tile = e.tile, g = tile.element;
  while (g.lastChild) g.removeChild(g.lastChild);

  var means = cluster.means();
  means.sort(function(a, b) { return b.size - a.size; });
  for (var i = 0; i < means.length; i++) {
    var mean = means[i], point = g.appendChild(po.svg("circle"));
    console.log("cx "+ mean.x);
    console.log("cy "+ mean.y);
    console.log("r "+ Math.pow(2, tile.zoom - 11) * Math.sqrt(mean.size));
    point.setAttribute("cx", 232.06164100746778);
    point.setAttribute("cy", 47.01318527401114);
    point.setAttribute("r", 13.856406460551018);//Math.pow(2, tile.zoom - 11) * Math.sqrt(mean.size));
  }// /
}*/

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
 * finalizing the newly added the rectangle, i.e. pinning it to the map and store the coordinates topLeft-bottomRight
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
  
  /* //useless stuff to show the coordinates on the side
  var div = document.createElement('div');
  div.id = "id"+idcount;
  div.innerHTML = topLeft+" "+bottomRight;
  console.log(vennDiv);
  console.log(div);
  var venns = document.getElementsByClassName("venns");
  console.log(venns);
  console.log(venns[0]);
  console.log(venns);
  //venns[0].appendChild(div);
  idcount++;
  */
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
    console.log(region);
    console.log(topLeft);
    console.log(bottomRight);
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
  consol.log("heelo");
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
}

function debug() {
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
}
debug(); //only used for debugging
