var root = null;
var connection = null;
var notification = null;

function start(rootClientWeb) {
  root = rootClientWeb;
  connection = new WebSocket("ws://localhost:9999/vertx/hot");
  connection.onmessage = onWebSocketMessage;
}

function finish() {
  connection.close()
}

function onWebSocketMessage(evt) {
  var message = JSON.parse(evt.data);
  switch(message.status) {
  case "COMPILING":
    notifyCompiling(message);
    break;
  case "DEPLOYING":
    notifyDeploying(message);
    break;
  case "DEPLOYED":
    notifyDeployed(message);
    break;
  case "STOPPED":
    notifyStopped(message);
    break;
  }
  if (message.status == "DEPLOYED") {
    reload(message.url);
  }
}

function notifyCompiling(message) {
  closeNotification();
  ensureNotification();
  notification.update("message", "Compiling ...");
}



function notifyDeploying(message) {
  ensureNotification();
  notification.update('message', "Deploying...");
}

function notifyDeployed(message) {
  ensureNotification();
  notification.update("message", "Deployed");
  setTimeout(function() {
    closeNotification();
  }, 1000);
}

function onWebSocketError(error) {
  console.log("websocket error: " + error);
}

function reload(url) {
  try {
    var currentLocation = root;
    var iframe = document.getElementById('embedded');

    if (url != root) {
      root = url;
      iframe.src = url;
    } else {
      iframe.src = iframe.contentWindow.location.href;
    }
  } catch (err) {
    console.log("error!");
    console.log(err);
  }
}

function closeNotification() {
  if (notification != null) {
    notification.close();
    notification = null;
  }
}

function ensureNotification() {
  if (notification == null) {
    notification = $.notify({
      message: ""
    }, {
      allow_dismiss: false,
      type: "info"
    });
  }
}