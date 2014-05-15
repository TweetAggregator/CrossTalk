//var po = org.polymaps;

//var map = po.map()
    //.container(document.getElementById("map").appendChild(po.svg("svg")))
	map.zoom(3)
   		.zoomRange([3, 13]); //limit zoom level
	map.on("move", update2)

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
    /*console.log("DA GEO "+JSON.stringify(toPix));
    console.log("DA Border "+JSON.stringify(border));
    console.log("DA e"+JSON.stringify(e));
    console.log("DA nR "+ nR);*/
    res.push(JSON.parse('{"x":'+toPix.x+', "y":'+toPix.y+', "r": '+nR+', "d": '+e.d+'}'));
    
  }
  return res;
}

//Functions to draw
  //Expects input of the form {"x": _, "y": _, "r": _}
var colors = ["", "LightCoral", "RoyalBlue", "Purple"]
function drawCircle(entry, colorID) {
  var point = graph.appendChild(po.svg("circle"));
  point.setAttribute('style', 'fill:'+ colors[colorID] +"; "+point.getAttribute("fill"));
  point.setAttribute('style', 'opacity:'+entry.d +"; "+point.getAttribute("style"));
  point.setAttribute("cx", entry.x);
  point.setAttribute("cy", entry.y);
  point.setAttribute("r", entry.r);
}

  //Goes through the list of centers at one level
function drawCenters(clusts, colorID){
  for(var i = 0; i < clusts.length; i++) {
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
  var selectedZoom = document.getElementById("zoomSlider").value;
  drawCenters(generateData(rawData1.clusters[selectedZoom].centers), 1);
  drawCenters(generateData(rawData2.clusters[selectedZoom].centers), 2);
  drawCenters(generateData(rawData3.clusters[selectedZoom].centers), 3);

}

window.onload = function() {
  var slider = document.getElementById("zoomSlider");
  if (window.addEventListener){
    slider.addEventListener('change', update2);
  } else if (window.attachEvent){
    slider.attachEvent('onchange', update2);
  }
  update2(); //initi rendering
}
  //Tests drawCenters
//drawCenters(generateData(rawData1.clusters[0].centers), 1);
/*drawCenters(generateDate(rawData2.clusters[0].centers), 2);
drawCenters(generateDate(rawData3.clusters[0].centers), 3);  */
  //Tests
//var res = geoToPx(JSON.parse('{"x": 37.787, "y": -122.228}'));
//var res2 = geoToPx(JSON.parse('{"x": 37.787, "y": -132.228}'));
//var res2 = pxToGeo(JSON.parse('{"x": 758.9, "y": 160}'));

//drawCircle(JSON.parse('{"x": 0, "y": 300, "r": 30}'))
