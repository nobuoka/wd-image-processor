var args = JSON.parse(arguments[0]) || {
  route: [],
  width: 120,
  height: 240,
};
return new Promise(function (resolve, reject) {
  var route = args.route;
  for (var i = 0; i + 1 < route.length; i += 2) {
    var x = route[i];
    var y = route[i + 1]
    var e = document.createElement("span");
    e.style.width = "2px";
    e.style.height = "2px";
    e.style.backgroundColor = "red";
    e.style.position = "absolute";
    e.style.top = (parseInt(x) - 1) + "px";
    e.style.left = (parseInt(y) - 1) + "px";
    document.body.appendChild(e);
  }
  var mapElem = document.getElementById("map");
  mapElem.style.width = args.width + "px";
  mapElem.style.height = args.height + "px";
  var map = new ol.Map({
    target: 'map',
    layers: [
      new ol.layer.Tile({
        source: new ol.source.OSM()
      })
    ],
    view: new ol.View({
      center: ol.proj.fromLonLat([37.41, 8.82]),
      zoom: 4
    })
  });
  setTimeout(function () {
    resolve({
      targetElement: document.getElementById("map"),
    });
  }, 2000); // TODO : 指定秒数待つのではなく、地図の読み込みが完了したら返るようにする。
});
