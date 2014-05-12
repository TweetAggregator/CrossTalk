var map = L.map('map').setView([51.505, -0.09], 7);
        L.tileLayer('http://{s}.tile.cloudmade.com/3c674276894b4bd1bcb0e98ecbdf875b/997/256/{z}/{x}/{y}.png', {
          attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://cloudmade.com">CloudMade</a>',
          maxZoom: 18
        }).addTo(map);
var div = document.getElementById("map"),
    svg = div.appendChild(po.svg("svg"));
map.container(svg);
var interact = po.interact(); //create separate object for the focus so we can remove it when drawing a rectangle
//map.add(interact); //enable move and zoom events
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
var drawnItems = new L.FeatureGroup();
map.addLayer(drawnItems);

map.on('layeradd', function (e) {
drawnItems.addLayer(e.layer);
alert(JSON.stringify(e.layer.toGeoJSON()));
});

var circleFlag =0;
function drawCircleRegion(){
  //map.remove(interact);
  circleFlag=1;
}
div.addEventListener("mouseup", function(){
    if (circleFlag == 1) {
      //newRegionCircleFlag = 0;
      circleFlag= 0;
     // document.getElementById("map").getElementsByTagName("svg")[0].removeChild(g);
        var circle = L.circle([51.508, -0.11], 5000, {
            color: 'red',
            fillColor: '#f03',
            fillOpacity: 0.5
        }).addTo(map);
      //g.removeChild(rect); //the svg element is delete (it does not stick to the map)
      //a new element, which sticks to the map, gets created
/*      var endX = startX+parseInt(rect.getAttribute("width"));
      var endY = startY+parseInt(rect.getAttribute("height"));
      var topLeft = pxToGeo(JSON.parse('{"x":'+startX+', "y": '+startY+'}'))
      var bottomRight = pxToGeo(JSON.parse('{"x":'+endX+', "y": '+endY+'}'))*/

      map.add(interact)   
  }
}, false);