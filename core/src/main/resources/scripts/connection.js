function start() {
  var connection = new WebSocket("ws://localhost:9999/vertx/hot");
  connection.onmessage = onWebSocketMessage;
  connection.onclose = onWebSocketClose;
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
  case "FAILED":
    notifyFailed(message);
    break;
  case "STOPPED":
    notifyStopped(message);
    break;
  }
  if (message.status == "DEPLOYED") {
    reload(message.url);
  }
}

function onWebSocketClose() {
  setTimeout(start, 1000);
}

function notifyCompiling(message) {
  closeNotification();
  notify("Compiling");
}


function notifyDeploying(message) {
  notify("Deploying...");
}

function notifyDeployed(message) {
  notify("Deployed");
}
function notifyFailed(message) {
  notify("Compilation Failed");
  document.body.innerHTML="<pre>"+message.cause+"</pre>"
}

function notify(message) {
  document.title = message;
}

function onWebSocketError(error) {
  console.log("websocket error: " + error);
}

function reload(url) {
  location.reload();
}

function closeNotification() {
  notify("");
}


start();