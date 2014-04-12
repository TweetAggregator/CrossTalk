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

// +/- buttons on map
map.add(po.compass()
    .pan("none"));

// "svg" can hold children "g" which hold graphics to be shown on top of the map
var g;
var rect; //temporary element for drawing before we can 
var count = 0;
var flag = 0;
var newRegionFlag = 0;
var startX = 0;
var startY = 0;
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
div.addEventListener("mouseup", function(){
    if (flag == 1) {
      newRegionFlag = 0;
      flag = 0;
      g.removeChild(rect);
      var endX = startX+parseInt(rect.getAttribute("width"));
      var endY = startY+parseInt(rect.getAttribute("height"));
      var topLeft = pxToGeo(JSON.parse('{"x":'+startX+', "y": '+startY+'}'))
      var bottomRight = pxToGeo(JSON.parse('{"x":'+endX+', "y": '+endY+'}'))

      map
        .add(interact)
        
        .add(po.geoJson()
          .features([{geometry: {coordinates: [[[topLeft.lon, topLeft.lat], [topLeft.lon, bottomRight.lat], [bottomRight.lon, bottomRight.lat], [bottomRight.lon, topLeft.lat], [0, 0]]], type: "Polygon"}}])
          .on("load", load));    
  }
}, false);


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
  converts {"x":coordX, "y": coordY}
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
  var myString = "<br/>lat 0: ".concat(topR.lat.toString()) +
    "<br/>lon: ".concat(topR.lon) +
    "<br/>lat".concat(bottomL.lat) +
    "<br/>lon".concat(bottomL.lon);
  document.getElementById('myDiv').innerHTML = myString ;
}

function newRegion() {
  map.remove(interact);
  //document.getElementById("map").unbind("mousemove", false);
  flag = 1;
}

function reset() {
  
}

//map.on("update", update);
