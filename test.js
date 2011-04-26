
var net = require("net")

var c = net.createConnection(8080, "localhost").on("connect", function() {
      var ips = ["8.8.8.8", "92.241.170.203","213.165.64.71"]
      for (var i = 0; i!=ips.length; ++i) {
        c.write(ips[i]);
        c.write("\n");
      }
    }).on("data", function(data){
      var str = data.toString(),
          arr = str.split("\n")

      for (var i=0; i!= arr.length; ++i){
        console.log(JSON.parse(arr[i]))
      }      
    })
