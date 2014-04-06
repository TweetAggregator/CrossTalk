var po = org.polymaps;

var map = po.map()
var div = document.getElementById("map"),
    svg = div.appendChild(po.svg("svg"));
map.container(svg);
map.add(po.interact()); //enable move and zoom

map.add(po.image()
    .url(po.url("http://{S}tile.cloudmade.com"
    + "/1a1b06b230af4efdbb989ea99e9841af" // http://cloudmade.com/register
    + "/998/256/{Z}/{X}/{Y}.png")
    .hosts(["a.", "b.", "c.", ""])));

map.add(po.geoJson()
    .url("district.json")
    .id("district") );
    //.on("load", load));

map.add(po.compass()
    .pan("none"));


//getElementsByClassName
//getElementsByTagName
var g = document.getElementById("map").getElementsByTagName("svg")[0].appendChild(po.svg("g")); //there should only be one
//map.container(g);
var rect = g.appendChild(po.svg("rect"));
rect.setAttribute("width", "50%");
rect.setAttribute("height", "50%");
var div = document.getElementById("map");
var x = div.clientWidth / 2,
    y = div.clientHeight / 2;
g.setAttribute("transform", "translate(" + (x / 2) + "," + (y / 2) + ")");

console.log("here is g: "+g+svg);

function neverCalled(e) {
  //Cedric
  var cluster = e.tile.cluster || (e.tile.cluster = kmeans()
      .iterations(16)
      .size(64));

  for (var i = 0; i < e.features.length; i++) {
    cluster.add(e.features[i].data.geometry.coordinates);
  }

  var tile = e.tile, g = tile.element;
  while (g.lastChild) g.removeChild(g.lastChild);

  var means = cluster.means();
  means.sort(function(a, b) { return b.size - a.size; });
  for (var i = 0; i < means.length; i++) {
    var mean = means[i], point = g.appendChild(po.svg("circle"));
    point.setAttribute("cx", mean.x);
    point.setAttribute("cy", mean.y);
    point.setAttribute("r", Math.pow(2, tile.zoom - 11) * Math.sqrt(mean.size));
  }

  //other example
  var r = 20 * Math.pow(2, e.tile.zoom - 12);
  for (var i = 0; i < e.features.length; i++) {
    var c = n$(e.features[i].element),
        g = c.parent().add("svg:g", c);

    g.attr("transform", "translate(" + c.attr("cx") + "," + c.attr("cy") + ")");

    g.add("svg:circle")
        .attr("r", r)
        .attr("transform", "translate(" + r + ",0)skewX(-45)")
        .attr("opacity", .5)
        .attr("filter", "url(#shadow)");

    g.add(c
        .attr("fill", "url(#r1)")
        .attr("r", r)
        .attr("cx", null)
        .attr("cy", null));

    g.add("svg:circle")
        .attr("transform", "scale(.95,1)")
        .attr("fill", "url(#r2)")
        .attr("r", r);
  }
}

function bounds(features) {
  var i = -1,
      n = features.length,
      geometry,
      bounds = [{lon: Infinity, lat: Infinity}, {lon: -Infinity, lat: -Infinity}];
  while (++i < n) {
    geometry = features[i].data.geometry;
    boundGeometry[geometry.type](bounds, geometry.coordinates);
  }
  return bounds;
}

function boundPoint(bounds, coordinate) {
  var x = coordinate[0], y = coordinate[1];
  if (x < bounds[0].lon) bounds[0].lon = x;
  if (x > bounds[1].lon) bounds[1].lon = x;
  if (y < bounds[0].lat) bounds[0].lat = y;
  if (y > bounds[1].lat) bounds[1].lat = y;
}

function boundPoints(bounds, coordinates) {
  var i = -1, n = coordinates.length;
  while (++i < n) boundPoint(bounds, coordinates[i]);
}

function boundMultiPoints(bounds, coordinates) {
  var i = -1, n = coordinates.length;
  while (++i < n) boundPoints(bounds, coordinates[i]);
}

var boundGeometry = {
  Point: boundPoint,
  MultiPoint: boundPoints,
  LineString: boundPoints,
  MultiLineString: boundMultiPoints,
  Polygon: function(bounds, coordinates) {
    boundPoints(bounds, coordinates[0]); // exterior ring
  },
  MultiPolygon: function(bounds, coordinates) {
    var i = -1, n = coordinates.length;
    while (++i < n) boundPoints(bounds, coordinates[i][0]);
  }
};

function update() {
  var topR = map.pointLocation(JSON.parse('{"x":0, "y": 0}'));
  //console.log(topR);
  var bottomL = map.pointLocation(map.size());
  //console.log(bottomL);
  var myString = "<br/>lat 0: ".concat(topR.lat.toString()) +
    "<br/>lo: ".concat(topR.lon) +
    "<br/>la".concat(bottomL.lat) +
    "<br/>l".concat(bottomL.lon);
  //console.log(myString);
  document.getElementById('myDiv').innerHTML = myString ;
 
  /*
  console.log(map.center());

  var gps = map.centerRange(); //center
  console.log(gps);
  console.log(gps[0].lat);
  console.log(map.locationCoordinate(map.center()).column);

  //var el = document.getElementById('insertHere'); el.html = '<div>Print this after the script tag</div>';
  //document.getElementById('inner').innerHTML = "Hello World!";
  //$("body").html(html);

  */

}

function addGeom() {}

function region() {
var mean = means[i], point = g.appendChild(po.svg("circle"));
  point.setAttribute("cx", mean.x);
  point.setAttribute("cy", mean.y);
  point.setAttribute("r", Math.pow(2, tile.zoom - 11) * Math.sqrt(mean.size));
}

map.on("resize", update);
map.on("move", update);
//map.on("update", update);
// => ou utiliser un boutton 
