//var po = org.polymaps;

//var map = po.map()
    //.container(document.getElementById("map").appendChild(po.svg("svg")))
    map.center({lat: 37.787, lon: -122.228})
    .zoom(7)
    .zoomRange([3, 10]);
map.on("move", update2);


//The variables for the display
  //TODO this has to be furnished by scala !
  //Maybe even 3 different lists actually ... nope ?
  //We expect {"clusters": [{"centers": [{"x":_, "y":_, "r":_}]}]} where all vars are in geoCoordinates
var rawData = JSON.parse('{"clusters": [{"centers": [{"x": 37.787, "y": -122.228, "r": -228.30}]}]}');
console.log(JSON.stringify(rawData));
  //TODO this has to be generated by calling the correct method!
var data = JSON.parse('{"clusters":[{"centers": [{ "x": 250, "y": 40, "r": 10 }, { "x": 40, "y": 75, "r": 43 }]}, {"centers": [{ "x": 759, "y": 255, "r": 36 }]}, {"centers": [{ "x": 50, "y": 40, "r": 10 }, { "x": 60, "y": 20, "r": 10 }]}, {"centers": [{ "x": 133, "y": 54, "r": 12 }, { "x": 40, "y": 75, "r": 43 }]}, {"centers":[{ "x": 260, "y": 120, "r": 10 }, { "x": 40, "y": 75, "r": 13 }, { "x": 33, "y": 54, "r": 12 }]}]}');

  //Graphical environnement
var graph = document.getElementById("map").getElementsByTagName("svg")[0].appendChild(po.svg("g"));


function cluster(view_center, zoom, data) {
  rawData = JSON.parse(data); update2();
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
    var border = geoToPx(JSON.parse('{"x": '+e.x+', "y": '+e.r+'}'));
    var toPix = geoToPx(e)
    console.log("Border "+JSON.stringify(border)+" toPix");
    var nR = Math.abs(border.y - toPix.y);
    res.push(JSON.parse('{"x":'+toPix.x+', "y":'+toPix.y+', "r": '+nR+'}'));
    console.log("The toPix.x "+toPix.x+" toPix.y "+toPix.y+" the nR "+nR);
  }
  console.log("Bitching hard");
  return res;
}

//Functions to draw
  //Expects input of the form {"x": _, "y": _, "r": _}
function drawCircle(entry) {
  console.log("Here shit");
  var point = graph.appendChild(po.svg("circle")); 
  point.setAttribute("cx", entry.x);
  point.setAttribute("cy", entry.y);
  point.setAttribute("r", entry.r);
}

  //Goes through the list of centers at one level
function drawCenters(clusts){
  for(var i = 0; i < clusts.length; i++) {
    console.log("Call it what you want");
    drawCircle(clusts[i]);
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
  console.log("GeoToPx x"+pt.x);
  return map.locationPoint(JSON.parse('{ "lat":'+pt.x+', "lon": '+pt.y+'}'));
}

function update2() {
	console.log("function update")
	if  (!map) {
		setTimeout(function() {update() }, 100) //busy wait while map is not yet loaded
	}
	var elem;
	topCorner = pxToGeo(JSON.parse('{"x":0, "y": 0}')); //TODO: pass to Play!
	bottomCorner = pxToGeo(map.size());
	elem = document.getElementById("viewBoundaries")
	if (elem)
		elem.value = JSON.stringify([topCorner, bottomCorner])
	elem = document.getElementById("viewCenter")
	if (elem)
		elem.value = JSON.stringify(map.center())
	elem = document.getElementById("zoomLevel")
	if (elem)
		elem.value = map.zoom()
}

//TEST to check everything works

  //Tests Draw circle
/*for(var i = 0; i < data.clusters[1].centers.length; i ++){
  var entry = data.clusters[1].centers[i];
  console.log("The shit I'm doing "+ entry.x + " The shit you do "+entry.y +" da fuck? "+entry.r);
  drawCircle(entry);
}*/

  //Tests drawCenters
drawCenters(generateData(rawData.clusters[0].centers));
  
  //Tests
var res = geoToPx(JSON.parse('{"x": 37.787, "y": -122.228}'));
var res2 = pxToGeo(JSON.parse('{"x": 758.9, "y": 160}'));
console.log("The x: "+res.x+" the y: "+res.y);
console.log("The x2:"+res2.lat+" the y2 "+res2.lon);
console.log("I'm here, I'm clear");

